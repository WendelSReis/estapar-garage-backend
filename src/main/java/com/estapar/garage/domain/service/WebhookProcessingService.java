package com.estapar.garage.domain.service;

import com.estapar.garage.api.dto.WebhookEventRequest;
import com.estapar.garage.domain.enums.ParkingSessionStatus;
import com.estapar.garage.domain.enums.SpotStatus;
import com.estapar.garage.domain.model.PricingResult;
import com.estapar.garage.exception.BusinessException;
import com.estapar.garage.exception.ResourceNotFoundException;
import com.estapar.garage.persistence.entity.ParkingSessionEntity;
import com.estapar.garage.persistence.entity.ParkingSpotEntity;
import com.estapar.garage.persistence.entity.SectorEntity;
import com.estapar.garage.persistence.repository.ParkingSessionRepository;
import com.estapar.garage.persistence.repository.ParkingSpotRepository;
import com.estapar.garage.persistence.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class WebhookProcessingService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final SectorRepository sectorRepository;
    private final ParkingPricingService parkingPricingService;

    public WebhookProcessingService(ParkingSessionRepository parkingSessionRepository,
                                    ParkingSpotRepository parkingSpotRepository,
                                    SectorRepository sectorRepository,
                                    ParkingPricingService parkingPricingService) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.sectorRepository = sectorRepository;
        this.parkingPricingService = parkingPricingService;
    }

    @Transactional
    public String process(WebhookEventRequest request) {
        return switch (request.event_type()) {
            case ENTRY -> processEntry(request);
            case PARKED -> processParked(request);
            case EXIT -> processExit(request);
        };
    }

    private String processEntry(WebhookEventRequest request) {
        if (request.entry_time() == null) {
            throw new BusinessException("Evento ENTRY deve possuir entry_time");
        }

        parkingSessionRepository.findCurrentSession(request.license_plate())
                .ifPresent(session -> {
                    throw new BusinessException("Veículo já possui sessão ativa");
                });

        ParkingSpotEntity reservedSpot = parkingSpotRepository.findFirstFreeSpotForUpdate()
                .orElseThrow(() -> new BusinessException("Garagem lotada. Nenhuma nova entrada é permitida"));

        SectorEntity sector = sectorRepository.findBySectorCodeForUpdate(reservedSpot.getSector().getSectorCode())
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));

        long occupiedBeforeEntry = parkingSpotRepository.countBySector_SectorCodeAndStatus(sector.getSectorCode(), SpotStatus.OCCUPIED);
        if (occupiedBeforeEntry >= sector.getMaxCapacity()) {
            throw new BusinessException("Setor lotado. Entrada não permitida até uma saída liberar vaga");
        }

        BigDecimal multiplier = parkingPricingService.resolveMultiplier(occupiedBeforeEntry, sector.getMaxCapacity());
        BigDecimal hourlyRate = sector.getBasePrice()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

        reservedSpot.setStatus(SpotStatus.OCCUPIED);
        parkingSpotRepository.save(reservedSpot);

        ParkingSessionEntity session = new ParkingSessionEntity();
        session.setId(UUID.randomUUID());
        session.setLicensePlate(request.license_plate());
        session.setEntryTime(request.entry_time());
        session.setSpot(reservedSpot);
        session.setSector(sector);
        session.setStatus(ParkingSessionStatus.ENTERED);
        session.setPriceMultiplier(multiplier);
        session.setHourlyRate(hourlyRate);
        parkingSessionRepository.save(session);

        return "ENTRY processado";
    }

    private String processParked(WebhookEventRequest request) {
        if (request.lat() == null || request.lng() == null) {
            throw new BusinessException("Evento PARKED deve possuir lat e lng");
        }

        ParkingSessionEntity session = parkingSessionRepository.findCurrentSession(request.license_plate())
                .orElseThrow(() -> new ResourceNotFoundException("Sessão ativa não encontrada para a placa"));

        if (session.getStatus() == ParkingSessionStatus.PARKED) {
            return "PARKED ignorado. Veículo já estacionado";
        }

        ParkingSpotEntity reservedSpot = session.getSpot();
        if (reservedSpot == null) {
            throw new BusinessException("Sessão sem vaga reservada");
        }

        ParkingSpotEntity lockedReservedSpot = parkingSpotRepository.findByIdForUpdate(reservedSpot.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vaga reservada não encontrada"));

        boolean sameLatitude = lockedReservedSpot.getLat().compareTo(request.lat()) == 0;
        boolean sameLongitude = lockedReservedSpot.getLng().compareTo(request.lng()) == 0;

        if (!sameLatitude || !sameLongitude) {
            throw new BusinessException("Coordenadas do PARKED não correspondem à vaga reservada no ENTRY");
        }

        if (lockedReservedSpot.getStatus() != SpotStatus.OCCUPIED) {
            throw new BusinessException("Vaga reservada não está ocupada no momento do PARKED");
        }

        session.setSpot(lockedReservedSpot);
        session.setSector(lockedReservedSpot.getSector());
        session.setParkedAt(Instant.now());
        session.setStatus(ParkingSessionStatus.PARKED);
        parkingSessionRepository.save(session);
        return "PARKED processado";
    }

    private String processExit(WebhookEventRequest request) {
        if (request.exit_time() == null) {
            throw new BusinessException("Evento EXIT deve possuir exit_time");
        }

        ParkingSessionEntity session = parkingSessionRepository.findCurrentSession(request.license_plate())
                .orElseThrow(() -> new ResourceNotFoundException("Sessão ativa não encontrada para a placa"));

        if (session.getSpot() != null) {
            ParkingSpotEntity spot = parkingSpotRepository.findByIdForUpdate(session.getSpot().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vaga da sessão não encontrada"));
            spot.setStatus(SpotStatus.FREE);
            parkingSpotRepository.save(spot);
        }

        PricingResult pricingResult = parkingPricingService.calculate(
                session.getSector().getBasePrice(),
                session.getPriceMultiplier() == null ? BigDecimal.ONE : session.getPriceMultiplier(),
                session.getEntryTime(),
                request.exit_time()
        );

        session.setExitTime(request.exit_time());
        session.setStatus(ParkingSessionStatus.EXITED);
        session.setChargedHours(pricingResult.chargedHours());
        session.setAmount(pricingResult.amount());
        if (session.getHourlyRate() == null) {
            session.setHourlyRate(pricingResult.hourlyRate());
        }
        parkingSessionRepository.save(session);
        return "EXIT processado";
    }
}
