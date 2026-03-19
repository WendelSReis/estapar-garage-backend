package com.estapar.garage.persistence.repository;

import com.estapar.garage.persistence.entity.SectorEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SectorRepository extends JpaRepository<SectorEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SectorEntity s where s.sectorCode = :sectorCode")
    Optional<SectorEntity> findBySectorCodeForUpdate(@Param("sectorCode") String sectorCode);
}
