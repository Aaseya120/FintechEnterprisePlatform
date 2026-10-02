package com.banking.bill.controller;

import com.banking.bill.domain.BillerCategory;
import com.banking.bill.dto.BillDtos.*;
import com.banking.bill.service.BillerInquiryService;
import com.banking.bill.service.BillSettlementService;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bills")
@Tag(name = "Bill Payment & Presentment", description = "Utility, Telecom, and Municipality Bill Inquiry and Settlement APIs")
public class BillPaymentController {

    private final BillerInquiryService inquiryService;
    private final BillSettlementService settlementService;

    public BillPaymentController(BillerInquiryService inquiryService,
                                 BillSettlementService settlementService) {
        this.inquiryService = inquiryService;
        this.settlementService = settlementService;
    }

    @GetMapping("/billers")
    @Operation(summary = "List Active Billers", description = "Retrieves all supported billers, optionally filtered by category")
    public ResponseEntity<ApiResponse<List<BillerResponseDto>>> getBillers(
            @RequestParam(required = false) BillerCategory category) {
        List<BillerResponseDto> billers = inquiryService.getActiveBillers(category);
        return ResponseEntity.ok(ApiResponse.success(billers, "Billers retrieved successfully"));
    }

    @PostMapping("/inquire")
    @Operation(summary = "Inquire Bill Due Amount", description = "Fetches real-time bill presentment and due amount from the biller gateway")
    public ResponseEntity<ApiResponse<BillInquiryResponseDto>> inquireBill(
            @Valid @RequestBody BillInquiryRequestDto request) {
        BillInquiryResponseDto response = inquiryService.inquireBill(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Bill inquiry retrieved successfully"));
    }

    @PostMapping("/pay")
    @Operation(summary = "Pay Bill", description = "Settles a utility or telecom bill using the customer's linked account")
    public ResponseEntity<ApiResponse<BillPaymentResponseDto>> payBill(
            @Valid @RequestBody BillPayRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        BillPaymentResponseDto response = settlementService.payBill(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Bill payment processed successfully"));
    }

    @GetMapping("/history/{customerId}")
    @Operation(summary = "Customer Bill Payment History", description = "Paged history of customer bill payments")
    public ResponseEntity<ApiResponse<Page<BillPaymentResponseDto>>> getHistory(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BillPaymentResponseDto> history = settlementService.getCustomerBillHistory(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history, "Customer bill history retrieved successfully"));
    }
}
