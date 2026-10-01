package com.banking.customer.repository;

import com.banking.customer.domain.CustomerActionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerActionAuditRepository extends JpaRepository<CustomerActionAuditLog, String> {

    List<CustomerActionAuditLog> findByCustomerIdOrderByTimestampDesc(String customerId);

    List<CustomerActionAuditLog> findByServiceIdOrderByTimestampDesc(String serviceId);
}
