package com.banking.notification.dto;

import com.banking.notification.domain.MobileDeviceRegistration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class MobileDeviceDtos {

    public record DeviceRegistrationRequestDto(
            @NotBlank(message = "Customer ID is required")
            String customerId,

            @NotNull(message = "Platform is required (IOS or ANDROID)")
            MobileDeviceRegistration.Platform platform,

            @NotBlank(message = "Device token is required")
            String deviceToken,

            String deviceModel,
            String osVersion,
            String appVersion,
            String biometricKey
    ) {}

    public record DeviceRegistrationResponseDto(
            String id,
            String customerId,
            MobileDeviceRegistration.Platform platform,
            String maskedDeviceToken,
            String deviceModel,
            String osVersion,
            String appVersion,
            boolean isActive,
            Instant registeredAt,
            Instant lastActiveAt
    ) {
        public static DeviceRegistrationResponseDto fromEntity(MobileDeviceRegistration reg) {
            String masked = reg.getDeviceToken().length() > 16
                    ? reg.getDeviceToken().substring(0, 8) + "..." + reg.getDeviceToken().substring(reg.getDeviceToken().length() - 8)
                    : "***";

            return new DeviceRegistrationResponseDto(
                    reg.getId(),
                    reg.getCustomerId(),
                    reg.getPlatform(),
                    masked,
                    reg.getDeviceModel(),
                    reg.getOsVersion(),
                    reg.getAppVersion(),
                    reg.isActive(),
                    reg.getRegisteredAt(),
                    reg.getLastActiveAt()
            );
        }
    }
}
