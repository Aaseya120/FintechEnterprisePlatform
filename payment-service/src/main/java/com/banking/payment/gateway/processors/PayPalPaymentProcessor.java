package com.banking.payment.gateway.processors;

import com.banking.payment.gateway.GatewayDtos.GatewayStatus;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayRequest;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayResponse;
import com.banking.payment.gateway.PaymentChannel;
import com.banking.payment.gateway.PaymentProcessorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Component
public class PayPalPaymentProcessor implements PaymentProcessorStrategy {

    private static final Logger log = LoggerFactory.getLogger(PayPalPaymentProcessor.class);

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.PAYPAL;
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request, String correlationId) {
        // Standard PayPal Merchant Fee calculation: 2.9% + 0.30
        BigDecimal fee = request.amount().multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        String payPalOrderId = "PAYID-" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();
        String txnRef = "PAYPAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        log.info("Processed PayPal payment [Payer: {}, Amount: {} {}, Fee: {}, Order: {}, corr: {}]",
                request.sourceAccountOrVpaOrCard(), request.amount(), request.currency(), fee, payPalOrderId, correlationId);

        return new PaymentGatewayResponse(
                txnRef,
                PaymentChannel.PAYPAL,
                request.amount(),
                request.currency(),
                GatewayStatus.SUCCESS,
                "PayPal order captured and settled. Net amount credited after platform gateway fee.",
                payPalOrderId,
                Instant.now()
        );
    }
}
