package com.estapar.garage.domain.service;

import com.estapar.garage.domain.enums.SpotStatus;
import com.estapar.garage.integration.simulator.GarageSimulatorClient;
import com.estapar.garage.integration.simulator.GarageSimulatorResponse;
import com.estapar.garage.persistence.entity.ParkingSpotEntity;
import com.estapar.garage.persistence.entity.SectorEntity;
import com.estapar.garage.persistence.repository.ParkingSpotRepository;
import com.estapar.garage.persistence.repository.SectorRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GarageSynchronizationService {

    private final GarageSimulatorClient simulatorClient;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public GarageSynchronizationService(GarageSimulatorClient simulatorClient,
                                        SectorRepository sectorRepository,
                                        ParkingSpotRepository parkingSpotRepository) {
        this.simulatorClient = simulatorClient;
        this.sectorRepository = sectorRepository;
        this.parkingSpotRepository = parkingSpotRepository;
    }

    @Transactional
    public void synchronizeGarage() {
        GarageSimulatorResponse response = simulatorClient.getGarageConfiguration();

        if (response == null) {
            throw new IllegalStateException("Resposta do simulador veio nula.");
        }

        if (response.garage() == null || response.garage().isEmpty()) {
            throw new IllegalStateException("Simulador não retornou setores da garagem.");
        }

        if (response.spots() == null || response.spots().isEmpty()) {
            throw new IllegalStateException("Simulador não retornou vagas da garagem.");
        }

        Map<String, SectorEntity> sectorsByCode = new HashMap<>();

        for (GarageSimulatorResponse.GarageSectorResponse sectorResponse : response.garage()) {
            if (sectorResponse.sector() == null || sectorResponse.sector().isBlank()) {
                throw new IllegalStateException("Setor inválido retornado pelo simulador.");
            }

            if (sectorResponse.basePrice() == null) {
                throw new IllegalStateException(
                        "Campo basePrice veio nulo para o setor " + sectorResponse.sector()
                );
            }

            if (sectorResponse.maxCapacity() == null) {
                throw new IllegalStateException(
                        "Campo maxCapacity veio nulo para o setor " + sectorResponse.sector()
                );
            }

            SectorEntity sector = sectorRepository.findById(sectorResponse.sector())
                    .orElseGet(SectorEntity::new);

            sector.setSectorCode(sectorResponse.sector());
            sector.setBasePrice(sectorResponse.basePrice());
            sector.setMaxCapacity(sectorResponse.maxCapacity());

            SectorEntity savedSector = sectorRepository.saveAndFlush(sector);
            sectorsByCode.put(savedSector.getSectorCode(), savedSector);
        }

        for (GarageSimulatorResponse.GarageSpotResponse spotResponse : response.spots()) {
            if (spotResponse.id() == null) {
                throw new IllegalStateException("Vaga inválida retornada pelo simulador: id nulo.");
            }

            if (spotResponse.sector() == null || spotResponse.sector().isBlank()) {
                throw new IllegalStateException(
                        "Vaga " + spotResponse.id() + " retornou sem setor."
                );
            }

            if (spotResponse.lat() == null || spotResponse.lng() == null) {
                throw new IllegalStateException(
                        "Vaga " + spotResponse.id() + " retornou com coordenadas inválidas."
                );
            }

            SectorEntity sector = sectorsByCode.get(spotResponse.sector());

            if (sector == null) {
                throw new IllegalStateException(
                        "Setor não encontrado para a vaga " + spotResponse.id()
                                + ". Setor recebido: " + spotResponse.sector()
                );
            }

            ParkingSpotEntity spot = parkingSpotRepository.findById(spotResponse.id())
                    .orElseGet(ParkingSpotEntity::new);

            spot.setId(spotResponse.id());
            spot.setSector(sector);
            spot.setLat(spotResponse.lat());
            spot.setLng(spotResponse.lng());

            if (spot.getStatus() == null) {
                spot.setStatus(SpotStatus.FREE);
            }

            parkingSpotRepository.save(spot);
        }
    }
}