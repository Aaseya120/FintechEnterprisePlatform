package com.banking.card.service;

import com.banking.card.domain.Card;
import com.banking.card.dto.CardDtos.*;
import com.banking.card.repository.CardRepository;
import com.banking.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

        kafkaTemplate.send("banking.card.issued", saved.getId(), saved);
        return mapToDto(saved);
    }

    @Transactional
    public CardResponseDto updateControls(String cardId, CardControlUpdateDto update) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        if (update.dailyLimit() != null) card.setDailyLimit(update.dailyLimit());
        if (update.isInternationalEnabled() != null) card.setInternationalEnabled(update.isInternationalEnabled());
        if (update.isContactlessEnabled() != null) card.setContactlessEnabled(update.isContactlessEnabled());

        Card saved = cardRepository.save(card);
        log.info("Updated card security controls for card ending in {}", getMasked(saved.getCardNumber()));
        return mapToDto(saved);
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
        kafkaTemplate.send("banking.card.blocked", card.getId(), reason);
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
        return com.banking.common.crypto.DataMaskingUtil.maskCardNumber(pan);
    }

    private CardResponseDto mapToDto(Card c) {
        return new CardResponseDto(
                c.getId(),
                com.banking.common.crypto.DataMaskingUtil.maskCardNumber(c.getCardNumber()),
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
                c.getCreatedAt()
        );
    }
}
