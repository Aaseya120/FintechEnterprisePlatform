package com.banking.common.audit;

import java.io.Serializable;
import java.time.Instant;

/**
 * Universal Customer Action Audit Event DTO:
 * Emitted by any microservice whenever a customer interacts with an account,
 * card, transfer, loan, or statement. Tagged with exact serviceId.
 */
public record CustomerActionAuditDto(
        String auditId,
        String customerId,
        String serviceId,
        String serviceName,
        String actionType,
        String resourceId,
        String details,
        String channel,
        String ipAddress,
        String status,
        Instant timestamp
) implements Serializable {

    public static CustomerActionAuditDto create(
            String customerId,
            BankingServiceRegistry service,
            String actionType,
            String resourceId,
            String details,
            String channel,
            String ipAddress,
            String status) {
        return new CustomerActionAuditDto(
                java.util.UUID.randomUUID().toString(),
                customerId,
                service.getServiceId(),
                service.getServiceName(),
                actionType,
                resourceId,
                details,
                channel != null ? channel : "WEB_PORTAL",
                ipAddress != null ? ipAddress : "127.0.0.1",
                status != null ? status : "SUCCESS",
                Instant.now()
        );
    }
}
