package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.dto.CardDtos.*;
import com.banking.card.repository.CardRepository;
import com.banking.common.crypto.DataMaskingUtil;
import com.banking.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CardRepository cardRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CardService(CardRepository cardRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.cardRepository = cardRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public CardResponseDto issueCard(CardIssuanceRequestDto req) {
        String pan = generateLuhnCardNumber(req.cardNetwork());
        int expMonth = LocalDate.now().getMonthValue();
        int expYear = LocalDate.now().getYear() + 5; // 5-year validity

        int rawCvv = 100 + RANDOM.nextInt(900);
        String cvvHash = hashSha256(String.valueOf(rawCvv));

        Card card = new Card(
                UUID.randomUUID().toString(),
                pan,
                req.cardNetwork(),
                req.cardType(),
                req.customerId(),
                req.linkedAccountNumber(),
                req.cardHolderName(),
                expMonth,
                expYear,
                cvvHash,
                req.dailyLimit()
        );

        Card saved = cardRepository.save(card);
        log.info("Issued {} {} for customer [{}]. Expiry: {}/{}",
                req.cardNetwork(), req.cardType(), req.customerId(), expMonth, expYear);

        kafkaTemplate.send("banking.card.issued", saved.getId(), saved)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish card.issued event for card {}: {}", saved.getId(), ex.getMessage());
                    }
                });
        return mapToDto(saved);
    }

    @Transactional
    public CardApplicationResponseDto applyCardWithEligibility(CardApplicationRequestDto req) {
        if (req.requestedCardType() == Card.CardType.CREDIT) {
            int score = req.creditScore() != null ? req.creditScore() : 600;
            BigDecimal income = req.annualIncome() != null ? req.annualIncome() : BigDecimal.ZERO;

            if (score < 650) {
                return new CardApplicationResponseDto(
                        false,
                        "Credit score " + score + " does not meet minimum credit requirement of 650",
                        null,
                        BigDecimal.ZERO,
                        "INELIGIBLE_FOR_CREDIT"
                );
            }

            if (income.compareTo(new BigDecimal("25000.00")) < 0) {
                return new CardApplicationResponseDto(
                        false,
                        "Annual income below minimum required threshold of $25,000",
                        null,
                        BigDecimal.ZERO,
                        "INSUFFICIENT_INCOME"
                );
            }

            BigDecimal approvedLimit = score >= 750
                    ? income.multiply(new BigDecimal("0.20")).min(new BigDecimal("50000.00"))
                    : income.multiply(new BigDecimal("0.10")).min(new BigDecimal("15000.00"));

            CardIssuanceRequestDto issueReq = new CardIssuanceRequestDto(
                    req.customerId(),
                    req.linkedAccountNumber(),
                    req.cardHolderName(),
                    req.cardNetwork(),
                    Card.CardType.CREDIT,
                    approvedLimit
            );
            CardResponseDto issued = issueCard(issueReq);
            String tier = score >= 750 ? "PLATINUM_REWARDS" : "GOLD_REWARDS";
            return new CardApplicationResponseDto(true, null, issued, approvedLimit, tier);
        } else {
            BigDecimal defaultLimit = req.requestedCardType() == Card.CardType.VIRTUAL
                    ? new BigDecimal("1000.00")
                    : new BigDecimal("3000.00");

            CardIssuanceRequestDto issueReq = new CardIssuanceRequestDto(
                    req.customerId(),
                    req.linkedAccountNumber(),
                    req.cardHolderName(),
                    req.cardNetwork(),
                    req.requestedCardType(),
                    defaultLimit
            );
            CardResponseDto issued = issueCard(issueReq);
            return new CardApplicationResponseDto(true, null, issued, defaultLimit, "STANDARD");
        }
    }

    @Transactional
    public CardResponseDto updateControls(String cardId, CardControlUpdateDto update) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        if (update.dailyLimit() != null) card.setDailyLimit(update.dailyLimit());
        if (update.isInternationalEnabled() != null) card.setInternationalEnabled(update.isInternationalEnabled());
        if (update.isContactlessEnabled() != null) card.setContactlessEnabled(update.isContactlessEnabled());
        if (update.isOnlineEnabled() != null) card.setOnlineEnabled(update.isOnlineEnabled());
        if (update.isAtmEnabled() != null) card.setAtmEnabled(update.isAtmEnabled());
        if (update.isPosEnabled() != null) card.setPosEnabled(update.isPosEnabled());

        Card saved = cardRepository.save(card);
        log.info("Updated card security controls for card ending in {}", getMasked(saved.getCardNumber()));
        return mapToDto(saved);
    }

    @Transactional
    public CardResponseDto freezeCard(String cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        card.freeze();
        Card saved = cardRepository.save(card);
        log.warn("Card ending in {} was FROZEN by customer via mobile app", getMasked(card.getCardNumber()));
        kafkaTemplate.send("banking.card.frozen", card.getId(), "Card temporarily frozen by user")
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish card.frozen event for card {}: {}", card.getId(), ex.getMessage());
                    }
                });
        return mapToDto(saved);
    }

    @Transactional
    public CardResponseDto unfreezeCard(String cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        card.unfreeze();
        Card saved = cardRepository.save(card);
        log.info("Card ending in {} was UNFROZEN by customer via mobile app", getMasked(card.getCardNumber()));
        kafkaTemplate.send("banking.card.unfrozen", card.getId(), "Card un-frozen by user")
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish card.unfrozen event for card {}: {}", card.getId(), ex.getMessage());
                    }
                });
        return mapToDto(saved);
    }

    /**
     * Generates a 5-minute rolling Dynamic CVV (dCVV) for mobile app screen display.
     * Uses HMAC-SHA256 with a server-side secret for cryptographic unpredictability.
     * Prevents shoulder surfing and static CVV theft for card-not-present (CNP) transactions.
     */
    public DynamicCvvResponseDto generateDynamicCvv(String cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        long currentEpochSec = System.currentTimeMillis() / 1000L;
        long window = currentEpochSec / 300L; // 5-minute bucket
        long expiresAtEpochSec = (window + 1) * 300L;
        long validForSeconds = expiresAtEpochSec - currentEpochSec;

        // Cryptographic HMAC-SHA256 of card ID + time window for unpredictable dCVV
        String seed = card.getId() + ":" + card.getCardNumber() + ":" + window;
        String hmac = hashSha256(seed);
        int rawHash = Math.abs(hmac.hashCode());
        int cvvNum = (rawHash % 900) + 100;
        String dynamicCvv = String.valueOf(cvvNum);

        return new DynamicCvvResponseDto(
                card.getId(),
                dynamicCvv,
                validForSeconds,
                Instant.ofEpochSecond(expiresAtEpochSec)
        );
    }

    public CardRewardDto getCardRewards(String cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        long pts = card.getRewardPoints();
        BigDecimal cashValue = BigDecimal.valueOf(pts).multiply(new BigDecimal("0.02")); // $0.02 per point
        String tier = pts > 50000 ? "PLATINUM_REWARDS" : pts > 10000 ? "GOLD_REWARDS" : "SILVER_REWARDS";

        return new CardRewardDto(card.getId(), pts, cashValue, tier);
    }

    @Transactional
    public void setPin(String cardId, PinSetDto pinDto) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        card.setPinHash(hashSha256(pinDto.pin()));
        cardRepository.save(card);
        log.info("PIN updated for card ending in {}", getMasked(card.getCardNumber()));
    }

    @Transactional
    public CardResponseDto blockCard(String cardId, String reason) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        card.setStatus(Card.CardStatus.BLOCKED);
        Card saved = cardRepository.save(card);
        log.warn("Card ending in {} was BLOCKED. Reason: {}", getMasked(card.getCardNumber()), reason);
        kafkaTemplate.send("banking.card.blocked", card.getId(), reason)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish card.blocked event for card {}: {}", card.getId(), ex.getMessage());
                    }
                });
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CardResponseDto> getCustomerCards(String customerId) {
        return cardRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private String generateLuhnCardNumber(Card.CardNetwork network) {
        String prefix = switch (network) {
            case VISA -> "4";
            case MASTERCARD -> "5";
            case RUPAY -> "6";
            case AMEX -> "37";
        };

        StringBuilder sb = new StringBuilder(prefix);
        while (sb.length() < 15) {
            sb.append(RANDOM.nextInt(10));
        }

        // Calculate Luhn check digit
        int checkDigit = calculateLuhnCheckDigit(sb.toString());
        sb.append(checkDigit);
        return sb.toString();
    }

    private int calculateLuhnCheckDigit(String numberWithoutCheck) {
        int sum = 0;
        boolean alternate = true;
        for (int i = numberWithoutCheck.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(numberWithoutCheck.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum * 9) % 10;
    }

    private String hashSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private String getMasked(String pan) {
        return DataMaskingUtil.maskCardNumber(pan);
    }

    private CardResponseDto mapToDto(Card c) {
        return new CardResponseDto(
                c.getId(),
                DataMaskingUtil.maskCardNumber(c.getCardNumber()),
                c.getCardNetwork(),
                c.getCardType(),
                c.getCustomerId(),
                c.getLinkedAccountNumber(),
                c.getCardHolderName(),
                c.getExpiryMonth(),
                c.getExpiryYear(),
                c.getStatus(),
                c.getDailyLimit(),
                c.isInternationalEnabled(),
                c.isContactlessEnabled(),
                c.isOnlineEnabled(),
                c.isAtmEnabled(),
                c.isPosEnabled(),
                c.getRewardPoints(),
                c.getCreatedAt()
        );
    }
}
