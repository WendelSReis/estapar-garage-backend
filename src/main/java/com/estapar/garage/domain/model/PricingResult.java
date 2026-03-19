package com.estapar.garage.domain.model;

import java.math.BigDecimal;

public record PricingResult(BigDecimal multiplier, BigDecimal hourlyRate, int chargedHours, BigDecimal amount) {
}
