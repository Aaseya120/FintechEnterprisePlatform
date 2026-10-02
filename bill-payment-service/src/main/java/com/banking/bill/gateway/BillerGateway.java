package com.banking.bill.gateway;

import com.banking.bill.domain.BillerCategory;
import com.banking.bill.dto.BillDtos.BillInquiryResponseDto;

import java.math.BigDecimal;

public interface BillerGateway {

    boolean supports(BillerCategory category);

    BillInquiryResponseDto inquireBill(String billerCode, String billerName, String consumerNumber, String currency);

    String settleBill(String billerCode, String consumerNumber, BigDecimal amount, String currency, String paymentReference);
}
