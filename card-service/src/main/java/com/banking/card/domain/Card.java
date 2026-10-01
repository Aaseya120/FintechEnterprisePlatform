package com.banking.card.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "cards",
    indexes = {
        @Index(name = "idx_cards_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_cards_account", columnList = "linked_account_number")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cards_number", columnNames = "card_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Card {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "card_number", length = 19, nullable = false, unique = true)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_network", length = 20, nullable = false)
    private CardNetwork cardNetwork;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", length = 20, nullable = false)
    private CardType cardType;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "linked_account_number", length = 34, nullable = false)
    private String linkedAccountNumber;

    @Column(name = "card_holder_name", length = 100, nullable = false)
    private String cardHolderName;

    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private int expiryYear;

    @Column(name = "cvv_hash", length = 64, nullable = false)
    private String cvvHash;

    @Column(name = "pin_hash", length = 64)
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CardStatus status;

    @Column(name = "daily_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal dailyLimit;

    @Column(name = "is_international_enabled", nullable = false)
    private boolean isInternationalEnabled;

    @Column(name = "is_contactless_enabled", nullable = false)
    private boolean isContactlessEnabled;

    @Column(name = "is_online_enabled", nullable = false)
    private boolean isOnlineEnabled;

    @Column(name = "is_atm_enabled", nullable = false)
    private boolean isAtmEnabled;

    @Column(name = "is_pos_enabled", nullable = false)
    private boolean isPosEnabled;

    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum CardNetwork { VISA, MASTERCARD, AMEX, RUPAY }
    public enum CardType { DEBIT, CREDIT, VIRTUAL }
    public enum CardStatus { ACTIVE, BLOCKED, FROZEN, EXPIRED }

    public Card(String id, String cardNumber, CardNetwork cardNetwork, CardType cardType,
                String customerId, String linkedAccountNumber, String cardHolderName,
                int expiryMonth, int expiryYear, String cvvHash, BigDecimal dailyLimit) {
        this.id = id;
        this.cardNumber = cardNumber;
        this.cardNetwork = cardNetwork;
        this.cardType = cardType;
        this.customerId = customerId;
        this.linkedAccountNumber = linkedAccountNumber;
        this.cardHolderName = cardHolderName;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvvHash = cvvHash;
        this.status = CardStatus.ACTIVE;
        this.dailyLimit = dailyLimit;
        this.isInternationalEnabled = false;
        this.isContactlessEnabled = true;
        this.isOnlineEnabled = true;
        this.isAtmEnabled = true;
        this.isPosEnabled = true;
        this.rewardPoints = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void freeze() {
        this.status = CardStatus.FROZEN;
        this.updatedAt = Instant.now();
    }

    public void unfreeze() {
        this.status = CardStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
}
