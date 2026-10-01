package com.banking.loan.repository;

import com.banking.loan.domain.LoanRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, String> {
    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(String loanId);
}
