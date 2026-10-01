package com.banking.batch.repository;

import com.banking.batch.model.StagingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StagingTransactionRepository extends JpaRepository<StagingTransaction, String> {

    List<StagingTransaction> findBySettlementDate(LocalDate settlementDate);

    List<StagingTransaction> findByBatchJobId(Long batchJobId);

    @Query("SELECT s FROM StagingTransaction s WHERE s.settlementDate = :date AND s.processingStatus = :status")
    List<StagingTransaction> findBySettlementDateAndStatus(@Param("date") LocalDate date, @Param("status") String status);
}
