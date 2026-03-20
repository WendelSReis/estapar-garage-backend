package com.estapar.garage.domain.service;

import com.estapar.garage.domain.model.PricingResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class ParkingPricingService {

    public BigDecimal resolveMultiplier(long occupiedBeforeEntry, int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Capacidade do setor deve ser maior que zero");
        }

        BigDecimal occupancy = BigDecimal.valueOf(occupiedBeforeEntry)
                .divide(BigDecimal.valueOf(maxCapacity), 4, RoundingMode.HALF_UP);

        if (occupancy.compareTo(BigDecimal.valueOf(0.25)) < 0) {
            return BigDecimal.valueOf(0.90);
        }
        if (occupancy.compareTo(BigDecimal.valueOf(0.50)) <= 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        if (occupancy.compareTo(BigDecimal.valueOf(0.75)) <= 0) {
            return BigDecimal.valueOf(1.10);
        }
        return BigDecimal.valueOf(1.25);
    }

    public PricingResult calculate(BigDecimal basePrice,
                                   BigDecimal multiplier,
                                   Instant entryTime,
                                   Instant exitTime) {
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Horário de saída não pode ser anterior ao de entrada");
        }

        BigDecimal hourlyRate = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();

        if (totalMinutes <= 30) {
            return new PricingResult(multiplier, hourlyRate, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        int chargedHours = (int) Math.ceil(totalMinutes / 60.0);
        BigDecimal amount = hourlyRate.multiply(BigDecimal.valueOf(chargedHours)).setScale(2, RoundingMode.HALF_UP);
        return new PricingResult(multiplier, hourlyRate, chargedHours, amount);
    }
}
