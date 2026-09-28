package com.enterprise.fintech.order.service;

import com.enterprise.fintech.grpc.PaymentRequest;
import com.enterprise.fintech.grpc.PaymentResponse;
import com.enterprise.fintech.grpc.PaymentServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcClient.class);

    @Value("${grpc.client.payment-service.address:static://localhost:9090}")
    private String grpcAddress;

    private ManagedChannel channel;
    private PaymentServiceGrpc.PaymentServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        String target = grpcAddress.replace("static://", "");
        String[] parts = target.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = PaymentServiceGrpc.newBlockingStub(channel);
        log.info("Initialized gRPC Channel to PaymentService at {}:{}", host, port);
    }

    public PaymentResponse processPayment(String orderId, String customerId, BigDecimal amount,
                                          String currency, String paymentMethod, String idempotencyKey) {
        log.info("Calling PaymentService via gRPC for orderId: {}, amount: {} {}", orderId, amount, currency);
        try {
            PaymentRequest request = PaymentRequest.newBuilder()
                    .setOrderId(orderId)
                    .setCustomerId(customerId)
                    .setAmount(amount.doubleValue())
                    .setCurrency(currency)
                    .setPaymentMethod(paymentMethod != null ? paymentMethod : "CARD")
                    .setIdempotencyKey(idempotencyKey != null ? idempotencyKey : "")
                    .build();

            return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).processPayment(request);
        } catch (Exception ex) {
            log.error("gRPC PaymentService call failed for orderId {}: {}", orderId, ex.getMessage());
            return PaymentResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus("FAILED")
                    .setMessage("gRPC call failed: " + ex.getMessage())
                    .build();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(3, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
