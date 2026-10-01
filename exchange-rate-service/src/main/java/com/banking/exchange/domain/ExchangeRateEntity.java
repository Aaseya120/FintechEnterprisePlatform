package com.banking.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "exchange_rates",
    indexes = {
        @Index(name = "idx_fx_pair", columnList = "from_currency, to_currency"),
        @Index(name = "idx_fx_updated", columnList = "last_updated_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fx_pair", columnNames = {"from_currency", "to_currency"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ExchangeRateEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency;

    @Column(name = "mid_rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal midRate;

    @Column(name = "bid_rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal bidRate; // Interbank buy rate

    @Column(name = "ask_rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal askRate; // Interbank sell rate

    @Column(name = "spread_percentage", precision = 6, scale = 4, nullable = false)
    private BigDecimal spreadPercentage;

    @Column(name = "change_24h_percentage", precision = 6, scale = 2, nullable = false)
    private BigDecimal change24hPercentage;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public ExchangeRateEntity(String id, String fromCurrency, String toCurrency,
                              BigDecimal midRate, BigDecimal bidRate, BigDecimal askRate,
                              BigDecimal spreadPercentage, BigDecimal change24hPercentage) {
        this.id = id;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.midRate = midRate;
        this.bidRate = bidRate;
        this.askRate = askRate;
        this.spreadPercentage = spreadPercentage;
        this.change24hPercentage = change24hPercentage;
        this.lastUpdatedAt = Instant.now();
    }
}
