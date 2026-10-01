package com.banking.card.controller;

import com.banking.card.dto.CardDtos.*;
import com.banking.card.service.CardService;
import com.banking.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "Card Management API", description = "Card Issuance, PIN Management, Security Controls & Blocking")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/issue")
    @Operation(summary = "Issue Debit or Credit Card with Luhn Verification")
    public ResponseEntity<ApiResponse<CardResponseDto>> issueCard(
            @Valid @RequestBody CardIssuanceRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardResponseDto response = cardService.issueCard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Card issued successfully", corrId));
    }

    @PutMapping("/{id}/controls")
    @Operation(summary = "Configure Security Controls (Domestic/International, Contactless, Limits)")
    public ResponseEntity<ApiResponse<CardResponseDto>> updateControls(
            @PathVariable String id,
            @RequestBody CardControlUpdateDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardResponseDto response = cardService.updateControls(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Card controls updated", corrId));
    }

    @PostMapping("/{id}/pin")
    @Operation(summary = "Set or Change Card PIN")
    public ResponseEntity<ApiResponse<String>> setPin(
            @PathVariable String id,
            @Valid @RequestBody PinSetDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        cardService.setPin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Card PIN updated successfully", corrId));
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Immediately Block Compromised or Lost Card")
    public ResponseEntity<ApiResponse<CardResponseDto>> blockCard(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        String reason = body.getOrDefault("reason", "Customer requested card block");
        CardResponseDto response = cardService.blockCard(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Card successfully blocked", corrId));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List Cards by Customer ID")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getCustomerCards(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<CardResponseDto> response = cardService.getCustomerCards(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }
}
