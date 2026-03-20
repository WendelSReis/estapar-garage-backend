package com.estapar.garage.integration.simulator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GarageSimulatorResponse(
        @JsonProperty("garage")
        List<GarageSectorResponse> garage,

        @JsonProperty("spots")
        List<GarageSpotResponse> spots
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GarageSectorResponse(
            @JsonProperty("sector")
            String sector,

            @JsonProperty("base_price")
            BigDecimal basePrice,

            @JsonProperty("max_capacity")
            Integer maxCapacity,

            @JsonProperty("open_hour")
            String openHour,

            @JsonProperty("close_hour")
            String closeHour,

            @JsonProperty("duration_limit_minutes")
            Integer durationLimitMinutes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GarageSpotResponse(
            @JsonProperty("id")
            Long id,

            @JsonProperty("sector")
            String sector,

            @JsonProperty("lat")
            BigDecimal lat,

            @JsonProperty("lng")
            BigDecimal lng
    ) {
    }
}