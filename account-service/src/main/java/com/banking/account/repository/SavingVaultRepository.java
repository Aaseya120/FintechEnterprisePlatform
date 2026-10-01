package com.banking.account.repository;

import com.banking.account.domain.SavingVault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingVaultRepository extends JpaRepository<SavingVault, String> {

    List<SavingVault> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<SavingVault> findByParentAccountNumber(String parentAccountNumber);
}
