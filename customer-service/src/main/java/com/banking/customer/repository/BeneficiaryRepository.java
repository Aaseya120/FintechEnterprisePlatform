package com.banking.customer.repository;

import com.banking.customer.domain.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, String> {
    List<Beneficiary> findByCustomerIdAndIsActiveTrue(String customerId);
    Optional<Beneficiary> findByIdAndCustomerId(String id, String customerId);
}
