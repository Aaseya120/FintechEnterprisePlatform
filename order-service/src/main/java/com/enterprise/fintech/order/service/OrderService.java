package com.enterprise.fintech.order.service;

import com.enterprise.fintech.common.event.OrderEvent;
import com.enterprise.fintech.grpc.PaymentResponse;
import com.enterprise.fintech.order.domain.Order;
import com.enterprise.fintech.order.domain.OrderStatus;
import com.enterprise.fintech.order.domain.OutboxEvent;
import com.enterprise.fintech.order.dto.CreateOrderRequest;
import com.enterprise.fintech.order.dto.OrderResponse;
import com.enterprise.fintech.order.repository.OrderRepository;
import com.enterprise.fintech.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MinioStorageService minioStorageService;
    private final PaymentGrpcClient paymentGrpcClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxEventRepository,
                        MinioStorageService minioStorageService,
                        PaymentGrpcClient paymentGrpcClient,
                        RedisTemplate<String, Object> redisTemplate,
                        ObjectMapper objectMapper,
                        Optional<Tracer> tracer) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.minioStorageService = minioStorageService;
        this.paymentGrpcClient = paymentGrpcClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tracer = tracer.orElse(null);
    }

    private String getCurrentTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return UUID.randomUUID().toString();
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public OrderResponse createOrder(CreateOrderRequest request) {
        String traceId = getCurrentTraceId();
        log.info("[Trace: {}] Processing order creation for customer: {}, amount: {}",
                traceId, request.getCustomerId(), request.getAmount());

        // 1. Idempotency Check via Redis / DB
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            String redisIdemKey = "idem:order:" + request.getIdempotencyKey();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisIdemKey, "PROCESSING", Duration.ofMinutes(10));
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("Duplicate request detected for idempotency key: {}", request.getIdempotencyKey());
                return orderRepository.findByIdempotencyKey(request.getIdempotencyKey())
                        .map(OrderResponse::fromEntity)
                        .orElseThrow(() -> new IllegalStateException("Order processing in progress"));
            }
        }

        // 2. Persist Order in PENDING state
        String orderRef = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Order order = new Order();
        order.setOrderReference(orderRef);
        order.setCustomerId(request.getCustomerId());
        order.setAmount(request.getAmount());
        order.setCurrency(request.getCurrency());
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setIdempotencyKey(request.getIdempotencyKey());
        order = orderRepository.save(order);

        // 3. Generate receipt and upload to MinIO
        String receiptContent = String.format(
                "=== FINTECH RECEIPT ===\nOrder: %s\nCustomer: %s\nAmount: %s %s\nDate: %s\nTrace: %s\n",
                orderRef, request.getCustomerId(), request.getAmount(), request.getCurrency(), Instant.now(), traceId
        );
        String invoiceUrl = minioStorageService.uploadOrderReceipt(orderRef, request.getCustomerId(), receiptContent);
        order.setMinioInvoiceUrl(invoiceUrl);
        order = orderRepository.save(order);

        // 4. Create Transactional Outbox Event (Guarantees At-Least-Once Delivery for Saga)
        try {
            OrderEvent orderEvent = new OrderEvent(
                    UUID.randomUUID().toString(),
                    order.getOrderReference(),
                    order.getCustomerId(),
                    order.getAmount(),
                    order.getCurrency(),
                    OrderEvent.EventType.ORDER_CREATED,
                    Instant.now(),
                    traceId
            );
            String payloadJson = objectMapper.writeValueAsString(orderEvent);
            OutboxEvent outboxEvent = new OutboxEvent("ORDER", orderRef, "ORDER_CREATED", payloadJson, traceId);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception ex) {
            log.error("Failed to serialize outbox event for order {}", orderRef, ex);
        }

        // 5. Synchronous gRPC Call to Payment Service (Fast Path with Saga Compensation)
        try {
            PaymentResponse paymentResponse = paymentGrpcClient.processPayment(
                    order.getOrderReference(),
                    order.getCustomerId(),
                    order.getAmount(),
                    order.getCurrency(),
                    request.getPaymentMethod(),
                    request.getIdempotencyKey()
            );

            if ("SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
                order.setStatus(OrderStatus.PAID);
                log.info("[Trace: {}] Payment succeeded for order {}", traceId, orderRef);
            } else {
                order.setStatus(OrderStatus.FAILED);
                log.warn("[Trace: {}] Payment failed for order {}: {}", traceId, orderRef, paymentResponse.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Payment gRPC call timed out or failed. Saga outbox background job will handle reconciliation: {}", ex.getMessage());
        }

        order = orderRepository.save(order);
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value = "orders", key = "#orderReference")
    public OrderResponse getOrderByReference(String orderReference) {
        log.info("Fetching order {} from Database (Cache Miss)", orderReference);
        return orderRepository.findByOrderReference(orderReference)
                .map(OrderResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with reference: " + orderReference));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @CachePut(value = "orders", key = "#orderReference")
    public OrderResponse compensateOrder(String orderReference, String reason) {
        log.info("Saga Compensation: Cancelling order {} due to: {}", orderReference, reason);
        Order order = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderReference));

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        return OrderResponse.fromEntity(order);
    }
}
