package com.banking.bill.service;

import com.banking.bill.domain.BillerCategory;
import com.banking.bill.dto.BillDtos.BillerResponseDto;
import com.banking.bill.dto.BillDtos.BillInquiryRequestDto;
import com.banking.bill.dto.BillDtos.BillInquiryResponseDto;

import java.util.List;

public interface BillerInquiryService {

    List<BillerResponseDto> getActiveBillers(BillerCategory category);

    BillInquiryResponseDto inquireBill(BillInquiryRequestDto request);
}
