package com.banking.payment.gateway;

import com.banking.common.exception.BankingException;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayRequest;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentGatewayManager {

    private final List<PaymentProcessorStrategy> strategies;

    public PaymentGatewayManager(List<PaymentProcessorStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentGatewayResponse dispatchPayment(PaymentGatewayRequest request, String correlationId) {
        PaymentProcessorStrategy processor = strategies.stream()
                .filter(s -> s.supports(request.channel()))
                .findFirst()
                .orElseThrow(() -> new BankingException(
                        "UNSUPPORTED_PAYMENT_CHANNEL",
                        "No payment processor found supporting channel: " + request.channel(),
                        HttpStatus.BAD_REQUEST));

        return processor.process(request, correlationId);
    }
}
