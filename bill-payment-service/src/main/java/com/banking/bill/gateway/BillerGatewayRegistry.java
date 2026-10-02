package com.banking.bill.gateway;

import com.banking.bill.domain.BillerCategory;
import com.banking.common.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BillerGatewayRegistry {

    private final List<BillerGateway> gateways;

    public BillerGatewayRegistry(List<BillerGateway> gateways) {
        this.gateways = gateways;
    }

    public BillerGateway getGateway(BillerCategory category) {
        return gateways.stream()
                .filter(gateway -> gateway.supports(category))
                .findFirst()
                .orElseThrow(() -> new BankingException(
                        "UNSUPPORTED_BILLER_CATEGORY",
                        "No gateway integration available for biller category: " + category,
                        HttpStatus.BAD_REQUEST
                ));
    }
}
