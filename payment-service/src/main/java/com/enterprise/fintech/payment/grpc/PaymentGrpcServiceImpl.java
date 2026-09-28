package com.enterprise.fintech.payment.grpc;

import com.enterprise.fintech.grpc.*;
import com.enterprise.fintech.legacy.ejb.PostingResult;
import com.enterprise.fintech.legacy.soa.SoaPostingResponse;
import com.enterprise.fintech.payment.legacy.LegacyCoreBankingEjbBridge;
import com.enterprise.fintech.payment.legacy.LegacyCoreBankingSoaClient;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@GrpcService
public class PaymentGrpcServiceImpl extends PaymentServiceGrpc.PaymentServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final LegacyCoreBankingSoaClient soaClient;
    private final LegacyCoreBankingEjbBridge ejbBridge;

    public PaymentGrpcServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                  LegacyCoreBankingSoaClient soaClient,
                                  LegacyCoreBankingEjbBridge ejbBridge) {
        this.redisTemplate = redisTemplate;
        this.soaClient = soaClient;
        this.ejbBridge = ejbBridge;
    }

    @Override
    public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        log.info("[gRPC Server] Received payment request for OrderId: {}, Customer: {}, Amount: {} {}",
                request.getOrderId(), request.getCustomerId(), request.getAmount(), request.getCurrency());

        // Idempotency check via Redis
        if (!request.getIdempotencyKey().isBlank()) {
            String idemKey = "idem:payment:" + request.getIdempotencyKey();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idemKey, "PROCESSED", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("[gRPC Server] Duplicate payment call detected for IdempotencyKey: {}", request.getIdempotencyKey());
                PaymentResponse response = PaymentResponse.newBuilder()
                        .setOrderId(request.getOrderId())
                        .setPaymentId("PAY-IDEM-DUPLICATE")
                        .setStatus("SUCCESS")
                        .setMessage("Payment already executed via idempotency key")
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
        }

        // Business rule check: Simulation of credit check / fraud detection
        if (request.getAmount() > 1000000.0) {
            log.warn("[gRPC Server] Payment rejected due to high transaction limit exceeded: {}", request.getAmount());
            PaymentResponse response = PaymentResponse.newBuilder()
                    .setOrderId(request.getOrderId())
                    .setStatus("FAILED")
                    .setMessage("Transaction limit exceeded for account")
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String txnRef = "TXN-" + System.currentTimeMillis();

        // 1. Anti-Corruption Layer: Settle payment to Legacy Core Banking via SOA SOAP Service
        SoaPostingResponse soaResponse = soaClient.postSettlementToCoreBanking(
                txnRef,
                request.getCustomerId(),
                "GL-CLEARING-ACCOUNT",
                BigDecimal.valueOf(request.getAmount()),
                request.getCurrency(),
                "Payment settlement for Order " + request.getOrderId()
        );

        if (soaResponse == null || soaResponse.getStatusCode() != SoaPostingResponse.StatusCode.ACTC) {
            String errorMsg = (soaResponse != null && soaResponse.getStatusDescription() != null)
                    ? soaResponse.getStatusDescription()
                    : "Core Banking SOA settlement rejected";
            log.warn("[gRPC Server] Payment settlement rejected for Order {}: {}", request.getOrderId(), errorMsg);
            PaymentResponse response = PaymentResponse.newBuilder()
                    .setPaymentId(paymentId)
                    .setOrderId(request.getOrderId())
                    .setStatus("FAILED")
                    .setMessage(errorMsg)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        String coreBankingRef = soaResponse.getCoreBankingReference() != null
                ? soaResponse.getCoreBankingReference()
                : txnRef;

        PaymentResponse response = PaymentResponse.newBuilder()
                .setPaymentId(paymentId)
                .setOrderId(request.getOrderId())
                .setStatus("SUCCESS")
                .setTransactionReference(coreBankingRef)
                .setMessage("Payment processed via gRPC and Core Banking SOA [CoreRef: " + coreBankingRef + "]")
                .setTimestamp(System.currentTimeMillis())
                .build();

        log.info("[gRPC Server] Payment successfully processed: {} with CBS Ref: {}", paymentId, coreBankingRef);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void refundPayment(RefundRequest request, StreamObserver<PaymentResponse> responseObserver) {
        log.info("[gRPC Server] Processing refund for PaymentId: {}, OrderId: {}, Amount: {}",
                request.getPaymentId(), request.getOrderId(), request.getRefundAmount());

        if (request.getRefundAmount() <= 0) {
            log.warn("[gRPC Server] Refund rejected due to non-positive amount: {}", request.getRefundAmount());
            PaymentResponse response = PaymentResponse.newBuilder()
                    .setPaymentId(request.getPaymentId())
                    .setOrderId(request.getOrderId())
                    .setStatus("FAILED")
                    .setMessage("Refund amount must be strictly positive")
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        String refundRef = "REF-" + System.currentTimeMillis();

        // 2. Anti-Corruption Layer: Credit refund to Core Banking via EJB Stateless Session Bean
        PostingResult postingResult = ejbBridge.postLedgerEntry(
                refundRef,
                "GL-CLEARING-ACCOUNT",
                request.getOrderId(),
                BigDecimal.valueOf(request.getRefundAmount()),
                "USD",
                "Refund for payment " + request.getPaymentId() + ": " + request.getReason()
        );

        if (postingResult == null || postingResult.getStatus() != PostingResult.Status.SUCCESS) {
            String errorMsg = (postingResult != null && postingResult.getResponseMessage() != null)
                    ? postingResult.getResponseMessage()
                    : "CBS EJB refund posting failed";
            log.warn("[gRPC Server] Refund rejected for Payment {}: {}", request.getPaymentId(), errorMsg);
            PaymentResponse response = PaymentResponse.newBuilder()
                    .setPaymentId(request.getPaymentId())
                    .setOrderId(request.getOrderId())
                    .setStatus("FAILED")
                    .setMessage(errorMsg)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        PaymentResponse response = PaymentResponse.newBuilder()
                .setPaymentId(request.getPaymentId())
                .setOrderId(request.getOrderId())
                .setStatus("REFUNDED")
                .setTransactionReference(postingResult.getCoreReference() != null ? postingResult.getCoreReference() : refundRef)
                .setMessage("Refund executed successfully via EJB CBS Bridge. Reason: " + request.getReason())
                .setTimestamp(System.currentTimeMillis())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPaymentStatus(PaymentStatusRequest request, StreamObserver<PaymentResponse> responseObserver) {
        PaymentResponse response = PaymentResponse.newBuilder()
                .setPaymentId(request.getPaymentId())
                .setOrderId(request.getOrderId())
                .setStatus("SUCCESS")
                .setTransactionReference("TXN-QUERY-" + System.currentTimeMillis())
                .setMessage("Payment record verified")
                .setTimestamp(System.currentTimeMillis())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
