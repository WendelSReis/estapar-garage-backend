package com.estapar.garage.api.dto;

import com.estapar.garage.domain.enums.EventType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record WebhookEventRequest(
        @NotBlank String license_plate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC") Instant entry_time,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC") Instant exit_time,
        BigDecimal lat,
        BigDecimal lng,
        @NotNull EventType event_type
) {
}
