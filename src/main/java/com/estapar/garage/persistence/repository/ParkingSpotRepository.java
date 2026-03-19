package com.estapar.garage.persistence.repository;

import com.estapar.garage.domain.enums.SpotStatus;
import com.estapar.garage.persistence.entity.ParkingSpotEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpotEntity, Long> {

    long countByStatus(SpotStatus status);

    long countBySector_SectorCodeAndStatus(String sectorCode, SpotStatus status);

    Optional<ParkingSpotEntity> findByLatAndLng(BigDecimal lat, BigDecimal lng);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ParkingSpotEntity p where p.id = :id")
    Optional<ParkingSpotEntity> findByIdForUpdate(@Param("id") Long id);
}
