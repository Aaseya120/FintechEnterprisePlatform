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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Component
public class UpiPaymentProcessor implements PaymentProcessorStrategy {

    private static final Logger log = LoggerFactory.getLogger(UpiPaymentProcessor.class);
    private static final BigDecimal MAX_UPI_TRANSACTION_LIMIT = new BigDecimal("100000.00");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.UPI;
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request, String correlationId) {
        String vpa = request.sourceAccountOrVpaOrCard();
        if (vpa == null || !vpa.contains("@")) {
            throw new BankingException("INVALID_VPA", "Invalid UPI Virtual Payment Address format. Expected: user@bank", HttpStatus.BAD_REQUEST);
        }

        if (request.amount().compareTo(MAX_UPI_TRANSACTION_LIMIT) > 0) {
            throw new BankingException("UPI_LIMIT_EXCEEDED",
                    "Transaction amount exceeds single UPI transaction regulatory cap of " + MAX_UPI_TRANSACTION_LIMIT,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Generate 12-digit NPCI standard Retrieval Reference Number (RRN)
        String rrn = String.valueOf(100000000000L + RANDOM.nextLong(900000000000L));
        String txnRef = "UPI_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        log.info("Processed UPI transaction [VPA: {}, Amount: {} {}, RRN: {}, corr: {}]",
                vpa, request.amount(), request.currency(), rrn, correlationId);

        return new PaymentGatewayResponse(
                txnRef,
                PaymentChannel.UPI,
                request.amount(),
                request.currency(),
                GatewayStatus.SUCCESS,
                "UPI payment debited and credited instantly via NPCI switch",
                "RRN:" + rrn,
                Instant.now()
        );
    }
}
