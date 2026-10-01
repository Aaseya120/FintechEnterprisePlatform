package com.banking.loan.repository;

import com.banking.loan.domain.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, String> {

    List<LoanRepayment> findByLoanIdOrderByPaidAtDesc(String loanId);

    List<LoanRepayment> findByCustomerIdOrderByPaidAtDesc(String customerId);
}
