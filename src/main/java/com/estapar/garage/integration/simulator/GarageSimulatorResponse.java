package com.estapar.garage.integration.simulator;

import java.math.BigDecimal;
import java.util.List;

public record GarageSimulatorResponse(List<GarageSectorResponse> garage,
                                      List<GarageSpotResponse> spots) {

    public record GarageSectorResponse(String sector, BigDecimal basePrice, Integer max_capacity) {
    }

    public record GarageSpotResponse(Long id, String sector, BigDecimal lat, BigDecimal lng) {
    }
}
