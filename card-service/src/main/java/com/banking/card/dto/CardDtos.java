package com.banking.card.dto;

import com.banking.card.domain.Card;
import io.swagger.v3.oas.annotations.media.Schema;
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
            Instant createdAt
    ) implements Serializable {}

    public record CardControlUpdateDto(
            BigDecimal dailyLimit,
            Boolean isInternationalEnabled,
            Boolean isContactlessEnabled
    ) implements Serializable {}

    public record PinSetDto(
            @NotBlank @Size(min = 4, max = 6) String pin
    ) implements Serializable {}
}
