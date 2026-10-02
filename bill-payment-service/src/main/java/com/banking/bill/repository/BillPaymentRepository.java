package com.banking.bill.repository;

import com.banking.bill.domain.BillPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, String> {

    Optional<BillPayment> findByPaymentReference(String paymentReference);

    Optional<BillPayment> findByIdempotencyKey(String idempotencyKey);

    Page<BillPayment> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
}
