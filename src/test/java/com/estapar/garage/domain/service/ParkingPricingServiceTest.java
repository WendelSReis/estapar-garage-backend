package com.estapar.garage.domain.service;

import com.estapar.garage.domain.model.PricingResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ParkingPricingServiceTest {

    private final ParkingPricingService service = new ParkingPricingService();

    @Test
    void shouldApplyDiscountWhenOccupancyIsBelow25Percent() {
        BigDecimal multiplier = service.resolveMultiplier(1, 10);
        assertThat(multiplier).isEqualByComparingTo("0.90");
    }

    @Test
    void shouldApplyDefaultPriceUpTo50Percent() {
        BigDecimal multiplier = service.resolveMultiplier(5, 10);
        assertThat(multiplier).isEqualByComparingTo("1.00");
    }

    @Test
    void shouldChargeZeroUpToThirtyMinutes() {
        PricingResult result = service.calculate(
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-01T10:30:00Z")
        );

        assertThat(result.chargedHours()).isZero();
        assertThat(result.amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRoundChargedHoursUpAfterGracePeriod() {
        PricingResult result = service.calculate(
                new BigDecimal("10.00"),
                new BigDecimal("1.10"),
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-01T11:01:00Z")
        );

        assertThat(result.hourlyRate()).isEqualByComparingTo("11.00");
        assertThat(result.chargedHours()).isEqualTo(2);
        assertThat(result.amount()).isEqualByComparingTo("22.00");
    }
}
