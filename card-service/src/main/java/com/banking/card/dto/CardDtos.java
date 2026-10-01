package com.banking.card.dto;

import com.banking.card.domain.Card;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class CardDtos {

    public record CardIssuanceRequestDto(
            @NotBlank String customerId,
            @NotBlank String linkedAccountNumber,
            @NotBlank String cardHolderName,
            @NotNull Card.CardNetwork cardNetwork,
            @NotNull Card.CardType cardType,
            @NotNull @DecimalMin("100.00") BigDecimal dailyLimit
    ) implements Serializable {}

    public record CardApplicationRequestDto(
            @NotBlank String customerId,
            @NotBlank String linkedAccountNumber,
            @NotBlank String cardHolderName,
            @NotNull Card.CardNetwork cardNetwork,
            @NotNull Card.CardType requestedCardType,
            BigDecimal annualIncome,
            Integer creditScore
    ) implements Serializable {}

    public record CardApplicationResponseDto(
            boolean approved,
            String rejectionReason,
            CardResponseDto issuedCard,
            BigDecimal approvedCreditLimit,
            String eligibleTier
    ) implements Serializable {}

    public record CardResponseDto(
            String id,
            String maskedCardNumber,
            Card.CardNetwork cardNetwork,
            Card.CardType cardType,
            String customerId,
            String linkedAccountNumber,
            String cardHolderName,
            int expiryMonth,
            int expiryYear,
            Card.CardStatus status,
            BigDecimal dailyLimit,
            boolean isInternationalEnabled,
            boolean isContactlessEnabled,
            boolean isOnlineEnabled,
            boolean isAtmEnabled,
            boolean isPosEnabled,
            long rewardPoints,
            Instant createdAt
    ) implements Serializable {}

    public record CardControlUpdateDto(
            BigDecimal dailyLimit,
            Boolean isInternationalEnabled,
            Boolean isContactlessEnabled,
            Boolean isOnlineEnabled,
            Boolean isAtmEnabled,
            Boolean isPosEnabled
    ) implements Serializable {}

    public record PinSetDto(
            @NotBlank @Size(min = 4, max = 6) String pin
    ) implements Serializable {}

    public record DynamicCvvResponseDto(
            String cardId,
            String dynamicCvv,
            long validForSeconds,
            Instant expiresAt
    ) implements Serializable {}

    public record CardRewardDto(
            String cardId,
            long rewardPoints,
            BigDecimal cashEquivalentValue,
            String tierStatus
    ) implements Serializable {}
}
