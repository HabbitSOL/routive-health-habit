package com.routive.healthhabit.drug.importer;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface DrugImportRunRepository extends JpaRepository<DrugImportRun, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from DrugImportRun r where r.id = :id")
    Optional<DrugImportRun> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
