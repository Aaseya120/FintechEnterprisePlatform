package com.banking.payment.gateway;

import com.banking.payment.gateway.GatewayDtos.PaymentGatewayRequest;
import com.banking.payment.gateway.GatewayDtos.PaymentGatewayResponse;

public interface PaymentProcessorStrategy {
    boolean supports(PaymentChannel channel);
    PaymentGatewayResponse process(PaymentGatewayRequest request, String correlationId);
}
