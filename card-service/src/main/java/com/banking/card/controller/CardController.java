package com.banking.card.controller;

import com.banking.card.dto.CardDtos.*;
import com.banking.card.service.CardService;
import com.banking.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for card lifecycle management, eligibility checks,
 * channel security toggles, mobile dynamic CVV, and loyalty rewards.
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Card Application with Real-Time Credit & Income Eligibility Evaluation.
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CardApplicationResponseDto>> applyCard(
            @Valid @RequestBody CardApplicationRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardApplicationResponseDto response = cardService.applyCardWithEligibility(request);
        HttpStatus status = response.approved() ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_ENTITY;
        String msg = response.approved() ? "Card approved and issued" : "Card application not approved";
        return ResponseEntity.status(status).body(ApiResponse.success(response, msg, corrId));
    }

    /**
     * Direct card issuance with Luhn check digit calculation.
     */
    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<CardResponseDto>> issueCard(
            @Valid @RequestBody CardIssuanceRequestDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardResponseDto response = cardService.issueCard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Card issued successfully", corrId));
    }

    /**
     * Configures domestic/international, contactless, ATM, POS, and online toggles.
     */
    @PutMapping("/{id}/controls")
    public ResponseEntity<ApiResponse<CardResponseDto>> updateControls(
            @PathVariable String id,
            @RequestBody CardControlUpdateDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardResponseDto response = cardService.updateControls(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Card controls updated", corrId));
    }

    /**
     * Sets or updates card PIN hash.
     */
    @PostMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<String>> setPin(
            @PathVariable String id,
            @Valid @RequestBody PinSetDto request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        cardService.setPin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Card PIN updated successfully", corrId));
    }

    /**
     * Permanently blocks a stolen or compromised card.
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<CardResponseDto>> blockCard(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        String reason = body.getOrDefault("reason", "Customer requested card block");
        CardResponseDto response = cardService.blockCard(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Card successfully blocked", corrId));
    }

    /**
     * Retrieves all cards issued to a customer.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getCustomerCards(
            @PathVariable String customerId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        List<CardResponseDto> response = cardService.getCustomerCards(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    /**
     * Mobile app temporary card freeze.
     */
    @PostMapping("/{id}/freeze")
    public ResponseEntity<ApiResponse<CardResponseDto>> freezeCard(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardResponseDto response = cardService.freezeCard(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Card frozen successfully", corrId));
    }

    /**
     * Mobile app card unfreeze restoring card to ACTIVE.
     */
    @PostMapping("/{id}/unfreeze")
    public ResponseEntity<ApiResponse<CardResponseDto>> unfreezeCard(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardResponseDto response = cardService.unfreezeCard(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Card un-frozen successfully", corrId));
    }

    /**
     * Generates a 5-minute rolling Dynamic CVV (dCVV) for mobile screen checkout.
     */
    @GetMapping("/{id}/dynamic-cvv")
    public ResponseEntity<ApiResponse<DynamicCvvResponseDto>> getDynamicCvv(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        DynamicCvvResponseDto response = cardService.generateDynamicCvv(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Dynamic CVV generated", corrId));
    }

    /**
     * Retrieves card loyalty points and cash equivalent value.
     */
    @GetMapping("/{id}/rewards")
    public ResponseEntity<ApiResponse<CardRewardDto>> getCardRewards(
            @PathVariable String id,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        CardRewardDto response = cardService.getCardRewards(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Card rewards retrieved", corrId));
    }
}
