package com.estapar.garage.persistence.repository;

import com.estapar.garage.domain.enums.ParkingSessionStatus;
import com.estapar.garage.persistence.entity.ParkingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSessionRepository extends JpaRepository<ParkingSessionEntity, UUID> {

    @Query("select s from ParkingSessionEntity s where s.licensePlate = :licensePlate and s.status in :statuses order by s.entryTime desc")
    List<ParkingSessionEntity> findActiveSessions(@Param("licensePlate") String licensePlate,
                                                  @Param("statuses") List<ParkingSessionStatus> statuses);

    default Optional<ParkingSessionEntity> findCurrentSession(String licensePlate) {
        return findActiveSessions(
                licensePlate,
                List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)
        ).stream().findFirst();
    }

    @Query("select coalesce(sum(s.amount), 0) from ParkingSessionEntity s where s.status = com.estapar.garage.domain.enums.ParkingSessionStatus.EXITED and s.sector.sectorCode = :sectorCode and s.exitTime >= :from and s.exitTime < :to")
    BigDecimal sumRevenueBySectorAndDate(@Param("sectorCode") String sectorCode,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to);
}
