package com.banking.bill.service;

import com.banking.bill.domain.Biller;
import com.banking.bill.domain.BillerCategory;
import com.banking.bill.domain.BillPayment;
import com.banking.bill.dto.BillDtos.*;
import com.banking.bill.gateway.BillerGateway;
import com.banking.bill.gateway.BillerGatewayRegistry;
import com.banking.bill.repository.BillerRepository;
import com.banking.bill.repository.BillPaymentRepository;
import com.banking.common.exception.BankingException;
import com.banking.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillPaymentServiceImpl implements BillerInquiryService, BillSettlementService {

    private static final Logger log = LoggerFactory.getLogger(BillPaymentServiceImpl.class);
    private static final String TOPIC_BILL_COMPLETED = "bill.payment.completed";
    private static final String TOPIC_BILL_FAILED = "bill.payment.failed";

    private final BillerRepository billerRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final BillerGatewayRegistry gatewayRegistry;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BillPaymentServiceImpl(BillerRepository billerRepository,
                                  BillPaymentRepository billPaymentRepository,
                                  BillerGatewayRegistry gatewayRegistry,
                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.billerRepository = billerRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.gatewayRegistry = gatewayRegistry;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillerResponseDto> getActiveBillers(BillerCategory category) {
        List<Biller> billers = (category != null)
                ? billerRepository.findByCategoryAndActiveTrue(category)
                : billerRepository.findByActiveTrue();

        return billers.stream()
                .map(this::mapToBillerDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BillInquiryResponseDto inquireBill(BillInquiryRequestDto request) {
        Biller biller = billerRepository.findByBillerCodeAndActiveTrue(request.billerCode())
                .orElseThrow(() -> new ResourceNotFoundException("Biller", request.billerCode()));

        BillerGateway gateway = gatewayRegistry.getGateway(biller.getCategory());
        return gateway.inquireBill(biller.getBillerCode(), biller.getBillerName(), request.consumerNumber(), biller.getCurrency());
    }

    @Override
    @Transactional
    public BillPaymentResponseDto payBill(BillPayRequestDto request, String idempotencyKey) {
        // Idempotency check: if already processed, return existing result
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<BillPayment> existing = billPaymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent hit for bill payment key: {}", idempotencyKey);
                return mapToPaymentDto(existing.get());
            }
        }

        Biller biller = billerRepository.findByBillerCodeAndActiveTrue(request.billerCode())
                .orElseThrow(() -> new ResourceNotFoundException("Biller", request.billerCode()));

        String paymentId = UUID.randomUUID().toString();
        String paymentRef = "BILL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        BigDecimal serviceFee = biller.getServiceFee() != null ? biller.getServiceFee() : BigDecimal.ZERO;

        BillPayment payment = new BillPayment(
                paymentId,
                paymentRef,
                request.customerId(),
                request.sourceAccountNumber(),
                request.billerCode(),
                request.consumerNumber(),
                request.amount(),
                request.currency(),
                serviceFee,
                idempotencyKey
        );
        payment = billPaymentRepository.save(payment);

        try {
            BillerGateway gateway = gatewayRegistry.getGateway(biller.getCategory());
            String billerTxnRef = gateway.settleBill(
                    biller.getBillerCode(),
                    request.consumerNumber(),
                    request.amount(),
                    request.currency(),
                    paymentRef
            );

            payment.markSuccess(billerTxnRef);
            payment = billPaymentRepository.save(payment);

            // Publish Kafka event asynchronously with failure logging callback
            BillPaymentResponseDto response = mapToPaymentDto(payment);
            kafkaTemplate.send(TOPIC_BILL_COMPLETED, paymentRef, response)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish Kafka event to [{}]: {}", TOPIC_BILL_COMPLETED, ex.getMessage(), ex);
                        } else {
                            log.debug("Successfully published event for bill payment: {}", paymentRef);
                        }
                    });

            return response;

        } catch (Exception ex) {
            log.error("Bill settlement failed for reference [{}]: {}", paymentRef, ex.getMessage());
            payment.markFailed(ex.getMessage());
            billPaymentRepository.save(payment);

            kafkaTemplate.send(TOPIC_BILL_FAILED, paymentRef, mapToPaymentDto(payment))
                    .whenComplete((res, kafkaEx) -> {
                        if (kafkaEx != null) {
                            log.error("Failed to publish failure event to [{}]: {}", TOPIC_BILL_FAILED, kafkaEx.getMessage());
                        }
                    });

            throw new BankingException("BILL_SETTLEMENT_FAILED", "Failed to settle bill with external provider: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BillPaymentResponseDto> getCustomerBillHistory(String customerId, Pageable pageable) {
        return billPaymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToPaymentDto);
    }

    private BillerResponseDto mapToBillerDto(Biller b) {
        return new BillerResponseDto(
                b.getId(),
                b.getBillerCode(),
                b.getBillerName(),
                b.getCategory(),
                b.getServiceFee(),
                b.getCurrency(),
                b.isActive()
        );
    }

    private BillPaymentResponseDto mapToPaymentDto(BillPayment p) {
        BigDecimal fee = p.getServiceFee() != null ? p.getServiceFee() : BigDecimal.ZERO;
        BigDecimal total = p.getAmount().add(fee);
        return new BillPaymentResponseDto(
                p.getPaymentReference(),
                p.getCustomerId(),
                p.getSourceAccountNumber(),
                p.getBillerCode(),
                p.getConsumerNumber(),
                p.getAmount(),
                fee,
                total,
                p.getCurrency(),
                p.getStatus(),
                p.getBillerTransactionRef(),
                p.getCreatedAt()
        );
    }
}
