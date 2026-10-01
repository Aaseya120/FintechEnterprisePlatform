package com.banking.customer.repository;

import com.banking.customer.domain.Beneficiary;
import com.banking.customer.domain.CustomerKyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerKycRepository extends JpaRepository<CustomerKyc, String> {
    List<CustomerKyc> findByCustomerId(String customerId);
    Optional<CustomerKyc> findByCustomerIdAndVerificationStatus(String customerId, CustomerKyc.KycStatus status);
    Optional<CustomerKyc> findTopByCustomerIdOrderByCreatedAtDesc(String customerId);
}
