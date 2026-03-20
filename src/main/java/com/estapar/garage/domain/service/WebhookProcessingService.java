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

        if (parkingSpotRepository.countByStatus(SpotStatus.FREE) == 0) {
            throw new BusinessException("Garagem lotada. Nenhuma nova entrada é permitida");
        }

        ParkingSessionEntity session = new ParkingSessionEntity();
        session.setId(UUID.randomUUID());
        session.setLicensePlate(request.license_plate());
        session.setEntryTime(request.entry_time());
        session.setStatus(ParkingSessionStatus.ENTERED);
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

        ParkingSpotEntity unlockedSpot = parkingSpotRepository.findByLatAndLng(request.lat(), request.lng())
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para coordenadas informadas"));
        ParkingSpotEntity spot = parkingSpotRepository.findByIdForUpdate(unlockedSpot.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para atualização"));

        if (spot.getStatus() == SpotStatus.OCCUPIED) {
            throw new BusinessException("Vaga já está ocupada");
        }

        SectorEntity sector = sectorRepository.findBySectorCodeForUpdate(spot.getSector().getSectorCode())
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));

        long occupiedBeforeEntry = parkingSpotRepository.countBySector_SectorCodeAndStatus(sector.getSectorCode(), SpotStatus.OCCUPIED);
        if (occupiedBeforeEntry >= sector.getMaxCapacity()) {
            throw new BusinessException("Setor lotado. Entrada não permitida até uma saída liberar vaga");
        }

        BigDecimal multiplier = parkingPricingService.resolveMultiplier(occupiedBeforeEntry, sector.getMaxCapacity());
        BigDecimal hourlyRate = sector.getBasePrice().multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);

        spot.setStatus(SpotStatus.OCCUPIED);
        session.setSpot(spot);
        session.setSector(sector);
        session.setParkedAt(Instant.now());
        session.setStatus(ParkingSessionStatus.PARKED);
        session.setPriceMultiplier(multiplier);
        session.setHourlyRate(hourlyRate);

        parkingSpotRepository.save(spot);
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

        PricingResult pricingResult;
        if (session.getSector() == null || session.getPriceMultiplier() == null) {
            pricingResult = new PricingResult(BigDecimal.ONE, BigDecimal.ZERO.setScale(2), 0, BigDecimal.ZERO.setScale(2));
        } else {
            pricingResult = parkingPricingService.calculate(
                    session.getSector().getBasePrice(),
                    session.getPriceMultiplier(),
                    session.getEntryTime(),
                    request.exit_time()
            );
        }

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
