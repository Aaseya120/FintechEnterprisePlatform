package com.banking.batch.reconciliation.repository;

import com.banking.batch.reconciliation.entity.ReconciliationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRunEntity, String> {

    List<ReconciliationRunEntity> findByReconciliationDateOrderByExecutedAtDesc(LocalDate date);

    List<ReconciliationRunEntity> findAllByOrderByExecutedAtDesc();
}
