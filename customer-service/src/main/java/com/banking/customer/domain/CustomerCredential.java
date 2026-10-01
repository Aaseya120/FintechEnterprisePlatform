package com.banking.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "customer_credentials",
    indexes = {
        @Index(name = "idx_cred_username", columnList = "username"),
        @Index(name = "idx_cred_customer", columnList = "customer_id"),
        @Index(name = "idx_cred_refresh_token", columnList = "refresh_token")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cred_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_cred_customer", columnNames = "customer_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CustomerCredential {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false, unique = true)
    private String customerId;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    private Instant refreshTokenExpiry;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CustomerCredential(String id, String customerId, String username, String passwordHash) {
        this.id = id;
        this.customerId = customerId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.mustChangePassword = true;
        this.failedLoginAttempts = 0;
        this.isLocked = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
