package com.banking.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "fx_rate_history",
    indexes = {
        @Index(name = "idx_fx_hist_pair_time", columnList = "from_currency, to_currency, recorded_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FxRateHistoryEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency;

    @Column(name = "rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal rate;

    @Column(name = "tick_type", length = 20, nullable = false)
    private String tickType;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public FxRateHistoryEntity(String id, String fromCurrency, String toCurrency, BigDecimal rate, String tickType) {
        this.id = id;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.tickType = tickType;
        this.recordedAt = Instant.now();
    }
}
