package com.banking.reporting.repository;

import com.banking.reporting.domain.ReportAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportAuditLogRepository extends JpaRepository<ReportAuditLog, String> {

    List<ReportAuditLog> findByAccountNumberOrderByExportedAtDesc(String accountNumber);

    List<ReportAuditLog> findTop20ByOrderByExportedAtDesc();
}
