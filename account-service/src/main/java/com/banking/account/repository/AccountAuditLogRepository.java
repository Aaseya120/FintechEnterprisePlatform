package com.banking.account.repository;

import com.banking.account.domain.AccountAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLog, String> {
    List<AccountAuditLog> findByAccountIdOrderByCreatedAtDesc(String accountId);
    Page<AccountAuditLog> findByAccountId(String accountId, Pageable pageable);
}
