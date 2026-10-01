package com.banking.payment.repository;

import com.banking.payment.domain.StandingInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StandingInstructionRepository extends JpaRepository<StandingInstruction, String> {

    List<StandingInstruction> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<StandingInstruction> findByStatusAndNextExecutionDateLessThanEqual(
            StandingInstruction.InstructionStatus status, LocalDate executionDate);
}
