package com.banking.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "customer_action_audits",
    indexes = {
        @Index(name = "idx_cust_audit_customer", columnList = "customer_id, timestamp"),
        @Index(name = "idx_cust_audit_service", columnList = "service_id, action_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CustomerActionAuditLog {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "service_id", length = 20, nullable = false)
    private String serviceId;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "channel", length = 30)
    private String channel;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public CustomerActionAuditLog(String id, String customerId, String serviceId, String serviceName,
                                  String actionType, String resourceId, String details,
                                  String channel, String ipAddress, String status, Instant timestamp) {
        this.id = id;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.actionType = actionType;
        this.resourceId = resourceId;
        this.details = details;
        this.channel = channel;
        this.ipAddress = ipAddress;
        this.status = status;
        this.timestamp = timestamp;
    }
}
