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

import java.time.Instant;
import java.util.UUID;

@Component
public class CardPaymentProcessor implements PaymentProcessorStrategy {

    private static final Logger log = LoggerFactory.getLogger(CardPaymentProcessor.class);

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.CARD;
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request, String correlationId) {
        String pan = request.sourceAccountOrVpaOrCard();
        if (pan == null || !isValidLuhn(pan)) {
            throw new BankingException("INVALID_CARD_NUMBER", "Card Primary Account Number (PAN) failed Luhn checksum validation", HttpStatus.BAD_REQUEST);
        }

        String cvv = request.metadata() != null ? request.metadata().get("cvv") : null;
        if (cvv == null || cvv.length() < 3) {
            throw new BankingException("INVALID_CVV", "Card CVV must be 3 or 4 digits", HttpStatus.BAD_REQUEST);
        }

        String authCode = "AUTH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String txnRef = "CARD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        log.info("Processed Card payment [Card ending in {}, Amount: {} {}, AuthCode: {}, corr: {}]",
                pan.substring(pan.length() - 4), request.amount(), request.currency(), authCode, correlationId);

        return new PaymentGatewayResponse(
                txnRef,
                PaymentChannel.CARD,
                request.amount(),
                request.currency(),
                GatewayStatus.SUCCESS,
                "Card authorization and capture approved via Payment Network (Visa/Mastercard)",
                authCode,
                Instant.now()
        );
    }

    private boolean isValidLuhn(String number) {
        String sanitized = number.replaceAll("\\s+", "");
        if (sanitized.length() < 13 || sanitized.length() > 19) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = sanitized.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(sanitized.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
