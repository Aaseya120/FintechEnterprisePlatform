package com.banking.payment.gateway.processors;

import com.banking.common.exception.BankingException;
import com.banking.payment.gateway.GatewayDtos.GatewayStatus;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayRequest;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayResponse;
import com.banking.payment.gateway.PaymentChannel;
import com.banking.payment.gateway.PaymentProcessorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class NetBankingPaymentProcessor implements PaymentProcessorStrategy {

    private static final Logger log = LoggerFactory.getLogger(NetBankingPaymentProcessor.class);
    private static final BigDecimal RTGS_MINIMUM_LIMIT = new BigDecimal("200000.00"); // Minimum 2 Lakhs / $25,000 for RTGS
    private static final BigDecimal IMPS_MAXIMUM_LIMIT = new BigDecimal("500000.00"); // 5 Lakhs maximum for IMPS

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.NETBANKING_IMPS
                || channel == PaymentChannel.NETBANKING_NEFT
                || channel == PaymentChannel.NETBANKING_RTGS;
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request, String correlationId) {
        PaymentChannel channel = request.channel();

        // Enforce RTGS minimum settlement threshold
        if (channel == PaymentChannel.NETBANKING_RTGS && request.amount().compareTo(RTGS_MINIMUM_LIMIT) < 0) {
            throw new BankingException("RTGS_THRESHOLD_NOT_MET",
                    "RTGS requires a minimum transaction amount of " + RTGS_MINIMUM_LIMIT + ". Use NEFT or IMPS for smaller amounts.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Enforce IMPS instant transfer ceiling
        if (channel == PaymentChannel.NETBANKING_IMPS && request.amount().compareTo(IMPS_MAXIMUM_LIMIT) > 0) {
            throw new BankingException("IMPS_MAX_EXCEEDED",
                    "IMPS allows a maximum instant transfer of " + IMPS_MAXIMUM_LIMIT + ". Use NEFT/RTGS for larger transfers.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String clearingRef = channel.name() + "_" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        String txnRef = "NETBANK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        String message = switch (channel) {
            case NETBANKING_IMPS -> "IMPS 24x7 instant fund transfer settled successfully";
            case NETBANKING_RTGS -> "RTGS high-value gross transaction cleared in real-time with central bank";
            case NETBANKING_NEFT -> "NEFT batch settlement queued for execution in next clearing cycle window";
            default -> "NetBanking transfer processed";
        };

        log.info("Processed NetBanking [{}] [Amount: {} {}, Ref: {}, corr: {}]",
                channel, request.amount(), request.currency(), clearingRef, correlationId);

        return new PaymentGatewayResponse(
                txnRef,
                channel,
                request.amount(),
                request.currency(),
                GatewayStatus.SUCCESS,
                message,
                clearingRef,
                Instant.now()
        );
    }
}
