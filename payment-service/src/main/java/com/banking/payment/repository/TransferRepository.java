package com.banking.payment.repository;

import com.banking.payment.domain.Transfer;
import com.banking.payment.domain.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, String> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    Optional<Transfer> findBySagaId(String sagaId);

    List<Transfer> findBySagaIdAndStatus(String sagaId, TransferStatus status);

    Page<Transfer> findBySourceAccountOrderByCreatedAtDesc(String sourceAccount, Pageable pageable);

    Page<Transfer> findByTargetAccountOrderByCreatedAtDesc(String targetAccount, Pageable pageable);
}
