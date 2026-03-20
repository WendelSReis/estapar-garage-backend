package com.estapar.garage.domain.service;

import com.estapar.garage.api.dto.RevenueResponse;
import com.estapar.garage.persistence.repository.ParkingSessionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class RevenueService {

    private final ParkingSessionRepository parkingSessionRepository;

    public RevenueService(ParkingSessionRepository parkingSessionRepository) {
        this.parkingSessionRepository = parkingSessionRepository;
    }

    public RevenueResponse getRevenue(LocalDate date, String sector) {
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal amount = parkingSessionRepository.sumRevenueBySectorAndDate(sector, from, to);
        return new RevenueResponse(amount.setScale(2), "BRL", Instant.now());
    }
}
