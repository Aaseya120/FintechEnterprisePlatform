package com.banking.batch.reconciliation.repository;

import com.banking.batch.reconciliation.entity.ReconciliationBreakEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationBreakRepository extends JpaRepository<ReconciliationBreakEntity, String> {

    List<ReconciliationBreakEntity> findByRunId(String runId);

    List<ReconciliationBreakEntity> findByResolutionStatus(String resolutionStatus);

    @Query("SELECT b FROM ReconciliationBreakEntity b WHERE b.resolutionStatus = 'OPEN' ORDER BY b.detectedAt DESC")
    List<ReconciliationBreakEntity> findAllOpenBreaks();

    @Query("SELECT COUNT(b) FROM ReconciliationBreakEntity b WHERE b.run.id = :runId AND b.resolutionStatus = 'OPEN'")
    long countOpenBreaksByRunId(@Param("runId") String runId);
}
