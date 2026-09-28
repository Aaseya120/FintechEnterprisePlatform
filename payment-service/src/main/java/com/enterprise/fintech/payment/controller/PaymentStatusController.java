package com.enterprise.fintech.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentStatusController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "payment-service",
                "grpc_port", 9090,
                "status", "HEALTHY",
                "active_protocol", "HTTP/2 (gRPC) & JMS (ActiveMQ)"
        ));
    }
}
