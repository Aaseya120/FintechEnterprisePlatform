package com.banking.account.repository;

import com.banking.account.domain.TermDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TermDepositRepository extends JpaRepository<TermDeposit, String> {

    List<TermDeposit> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    Optional<TermDeposit> findByDepositNumber(String depositNumber);

    List<TermDeposit> findByStatusAndMaturityDateLessThanEqual(TermDeposit.TermDepositStatus status, LocalDate date);
}
