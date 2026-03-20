package com.estapar.garage.domain.service;

import com.estapar.garage.domain.enums.SpotStatus;
import com.estapar.garage.integration.simulator.GarageSimulatorClient;
import com.estapar.garage.integration.simulator.GarageSimulatorResponse;
import com.estapar.garage.persistence.entity.ParkingSpotEntity;
import com.estapar.garage.persistence.entity.SectorEntity;
import com.estapar.garage.persistence.repository.ParkingSpotRepository;
import com.estapar.garage.persistence.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Map<String, SectorEntity> sectorsByCode = new HashMap<>();

        for (GarageSimulatorResponse.GarageSectorResponse sectorResponse : response.garage()) {
            SectorEntity sector = sectorRepository.findById(sectorResponse.sector())
                    .orElseGet(SectorEntity::new);
            sector.setSectorCode(sectorResponse.sector());
            sector.setBasePrice(sectorResponse.basePrice());
            sector.setMaxCapacity(sectorResponse.max_capacity());
            sectorRepository.save(sector);
            sectorsByCode.put(sector.getSectorCode(), sector);
        }

        for (GarageSimulatorResponse.GarageSpotResponse spotResponse : response.spots()) {
            ParkingSpotEntity spot = parkingSpotRepository.findById(spotResponse.id())
                    .orElseGet(ParkingSpotEntity::new);
            spot.setId(spotResponse.id());
            spot.setSector(sectorsByCode.get(spotResponse.sector()));
            spot.setLat(spotResponse.lat());
            spot.setLng(spotResponse.lng());
            if (spot.getStatus() == null) {
                spot.setStatus(SpotStatus.FREE);
            }
            parkingSpotRepository.save(spot);
        }
    }
}
