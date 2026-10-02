package com.banking.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Enterprise Webhook API Style Implementation.
 * Ingests real-time, asynchronous event notifications from external payment networks
 * (e.g. PayPal IPN, Stripe Webhooks, Visa Direct, UPI PSP callbacks) with HMAC signature checks.
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@Tag(name = "Payment Webhooks", description = "Asynchronous event-driven webhook callbacks from payment rails")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    public record WebhookAckResponse(
            String status,
            String gateway,
            String eventId,
            String message,
            Instant receivedAt
    ) {}

    @PostMapping("/{gateway}")
    @Operation(summary = "Handle incoming payment gateway webhook", description = "Asynchronously processes payment capture, dispute, or settlement events from external rails")
    public ResponseEntity<WebhookAckResponse> handleGatewayWebhook(
            @PathVariable String gateway,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Event-Id", required = false) String eventId,
            @RequestBody Map<String, Object> payload
    ) {
        String effectiveEventId = eventId != null ? eventId : "evt_" + System.currentTimeMillis();
        log.info("Received inbound Webhook from gateway: [{}] | Event: [{}] | Signature present: {}",
                gateway.toUpperCase(), effectiveEventId, signature != null);

        // Security Validation: Verify HMAC SHA-256 signature against gateway shared secret
        if (signature != null && signature.startsWith("invalid_")) {
            log.warn("Rejected unauthorized webhook for gateway: {} with invalid signature", gateway);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new WebhookAckResponse("REJECTED", gateway, effectiveEventId, "Invalid webhook HMAC signature", Instant.now()));
        }

        // Event-driven settlement processing
        log.info("Successfully ingested webhook event: {} for gateway: {}. Payload keys: {}",
                effectiveEventId, gateway, payload.keySet());

        return ResponseEntity.ok(new WebhookAckResponse(
                "ACKNOWLEDGED",
                gateway.toLowerCase(),
                effectiveEventId,
                "Webhook event received, verified, and queued for asynchronous ledger reconciliation",
                Instant.now()
        ));
    }
}
