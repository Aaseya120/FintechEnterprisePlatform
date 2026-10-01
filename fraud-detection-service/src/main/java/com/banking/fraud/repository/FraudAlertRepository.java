package com.banking.fraud.repository;

import com.banking.fraud.domain.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, String> {
    List<FraudAlert> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<FraudAlert> findByRiskDecision(FraudAlert.RiskDecision decision);
}
