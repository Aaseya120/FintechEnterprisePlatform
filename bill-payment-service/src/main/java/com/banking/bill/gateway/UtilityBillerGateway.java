package com.banking.bill.gateway;

import com.banking.bill.domain.BillerCategory;
import com.banking.bill.dto.BillDtos.BillInquiryResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UtilityBillerGateway implements BillerGateway {

    private static final Logger log = LoggerFactory.getLogger(UtilityBillerGateway.class);

    @Override
    public boolean supports(BillerCategory category) {
        return category == BillerCategory.UTILITY || category == BillerCategory.MUNICIPALITY;
    }

    @Override
    public BillInquiryResponseDto inquireBill(String billerCode, String billerName, String consumerNumber, String currency) {
        log.info("Inquiring utility bill for biller {} consumer {}", billerCode, consumerNumber);
        BigDecimal simulatedDue = BigDecimal.valueOf(30 + ThreadLocalRandom.current().nextInt(150));
        return new BillInquiryResponseDto(
                billerCode,
                billerName,
                consumerNumber,
                "Utility Subscriber",
                simulatedDue,
                currency,
                LocalDate.now().plusDays(15),
                "KW-METER-" + LocalDate.now().getYear()
        );
    }

    @Override
    public String settleBill(String billerCode, String consumerNumber, BigDecimal amount, String currency, String paymentReference) {
        log.info("Settling utility bill {} for consumer {} amount {} {}", billerCode, consumerNumber, amount, currency);
        return "UTL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
