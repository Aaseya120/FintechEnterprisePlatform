package com.banking.bill.service;

import com.banking.bill.dto.BillDtos.BillPayRequestDto;
import com.banking.bill.dto.BillDtos.BillPaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillSettlementService {

    BillPaymentResponseDto payBill(BillPayRequestDto request, String idempotencyKey);

    Page<BillPaymentResponseDto> getCustomerBillHistory(String customerId, Pageable pageable);
}
