package com.banking.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "mobile_device_registrations",
    indexes = {
        @Index(name = "idx_mobile_cust_active", columnList = "customer_id, is_active"),
        @Index(name = "idx_mobile_token", columnList = "device_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MobileDeviceRegistration {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20, nullable = false)
    private Platform platform;

    @Column(name = "device_token", length = 512, nullable = false)
    private String deviceToken;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "biometric_key", length = 512)
    private String biometricKey;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    public enum Platform { IOS, ANDROID }

    public MobileDeviceRegistration(String id, String customerId, Platform platform,
                                    String deviceToken, String deviceModel,
                                    String osVersion, String appVersion, String biometricKey) {
        this.id = id;
        this.customerId = customerId;
        this.platform = platform;
        this.deviceToken = deviceToken;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.biometricKey = biometricKey;
        this.isActive = true;
        this.registeredAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }
}
