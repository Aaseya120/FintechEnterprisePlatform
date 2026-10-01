package com.banking.notification.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.notification.domain.MobileDeviceRegistration;
import com.banking.notification.domain.NotificationLog;
import com.banking.notification.dto.MobileDeviceDtos.*;
import com.banking.notification.repository.MobileDeviceRegistrationRepository;
import com.banking.notification.repository.NotificationLogRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for notification audit history and mobile push device registration (APNs / FCM).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationLogRepository logRepository;
    private final MobileDeviceRegistrationRepository deviceRepository;

    public NotificationController(NotificationLogRepository logRepository,
                                  MobileDeviceRegistrationRepository deviceRepository) {
        this.logRepository = logRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Retrieves dispatched notification logs (SMS, Email, Push) for a recipient.
     */
    @GetMapping("/recipient/{recipient}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getCustomerNotifications(
            @PathVariable String recipient,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<NotificationLog> logs = logRepository.findByRecipientOrderBySentAtDesc(recipient);
        return ResponseEntity.ok(ApiResponse.success(logs, corrId));
    }

    /**
     * Binds customer mobile session to hardware device token (iOS APNs or Android FCM) and biometric key.
     */
    @PostMapping("/devices/register")
    public ResponseEntity<ApiResponse<DeviceRegistrationResponseDto>> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();

        MobileDeviceRegistration device = deviceRepository.findByDeviceToken(request.deviceToken())
                .orElseGet(() -> new MobileDeviceRegistration(
                        UUID.randomUUID().toString(),
                        request.customerId(),
                        request.platform(),
                        request.deviceToken(),
                        request.deviceModel(),
                        request.osVersion(),
                        request.appVersion(),
                        request.biometricKey()
                ));

        device.setActive(true);
        device.setLastActiveAt(Instant.now());
        if (request.deviceModel() != null) device.setDeviceModel(request.deviceModel());
        if (request.osVersion() != null) device.setOsVersion(request.osVersion());
        if (request.appVersion() != null) device.setAppVersion(request.appVersion());

        MobileDeviceRegistration saved = deviceRepository.save(device);
        var response = DeviceRegistrationResponseDto.fromEntity(saved);
        return ResponseEntity.ok(ApiResponse.success(response, "Mobile device registered for push notifications", corrId));
    }

    /**
     * Lists active registered devices for a customer.
     */
    @GetMapping("/devices/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<DeviceRegistrationResponseDto>>> getCustomerDevices(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<DeviceRegistrationResponseDto> list = deviceRepository.findByCustomerIdAndIsActiveTrue(customerId)
                .stream()
                .map(DeviceRegistrationResponseDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list, corrId));
    }

    /**
     * Deactivates mobile device push token upon logout.
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<String>> deactivateDevice(
            @PathVariable String deviceId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        deviceRepository.findById(deviceId).ifPresent(d -> {
            d.setActive(false);
            deviceRepository.save(d);
        });
        return ResponseEntity.ok(ApiResponse.success("Device push token deactivated", corrId));
    }
}
