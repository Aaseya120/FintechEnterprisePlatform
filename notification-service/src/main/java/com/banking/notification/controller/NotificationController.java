package com.banking.notification.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.notification.domain.NotificationLog;
import com.banking.notification.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification Audit API", description = "Query Dispatched SMS, Push & Email Alerts")
public class NotificationController {

    private final NotificationLogRepository logRepository;

    public NotificationController(NotificationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping("/recipient/{recipient}")
    @Operation(summary = "Get Customer Notification History")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getCustomerNotifications(
            @PathVariable String recipient,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<NotificationLog> logs = logRepository.findByRecipientOrderBySentAtDesc(recipient);
        return ResponseEntity.ok(ApiResponse.success(logs, corrId));
    }
}
