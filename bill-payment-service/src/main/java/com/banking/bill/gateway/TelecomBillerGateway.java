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
public class TelecomBillerGateway implements BillerGateway {

    private static final Logger log = LoggerFactory.getLogger(TelecomBillerGateway.class);

    @Override
    public boolean supports(BillerCategory category) {
        return category == BillerCategory.TELECOM;
    }

    @Override
    public BillInquiryResponseDto inquireBill(String billerCode, String billerName, String consumerNumber, String currency) {
        log.info("Inquiring telecom bill for biller {} consumer {}", billerCode, consumerNumber);
        BigDecimal simulatedDue = BigDecimal.valueOf(15 + ThreadLocalRandom.current().nextInt(85));
        return new BillInquiryResponseDto(
                billerCode,
                billerName,
                consumerNumber,
                "Telecom Customer",
                simulatedDue,
                currency,
                LocalDate.now().plusDays(10),
                "CYCLE-" + LocalDate.now().getMonthValue()
        );
    }

    @Override
    public String settleBill(String billerCode, String consumerNumber, BigDecimal amount, String currency, String paymentReference) {
        log.info("Settling telecom bill {} for consumer {} amount {} {}", billerCode, consumerNumber, amount, currency);
        return "TEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
