package com.estapar.garage.api.dto;

import java.time.Instant;

public record WebhookEventResponse(String message, Instant processedAt) {
}
