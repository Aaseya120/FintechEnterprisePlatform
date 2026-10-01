package com.banking.exchange.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
public class CountryEntity {

    @Id
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    @Column(name = "alpha3_code", length = 3, nullable = false)
    private String alpha3Code;

    @Column(name = "country_name", length = 100, nullable = false)
    private String countryName;

    @Column(name = "dialing_code", length = 10, nullable = false)
    private String dialingCode;

    @Column(name = "default_currency", length = 3, nullable = false)
    private String defaultCurrency;

    @Column(name = "iban_pattern", length = 100)
    private String ibanPattern;

    @Column(name = "iban_length")
    private Integer ibanLength;

    @Column(name = "swift_prefix", length = 4)
    private String swiftPrefix;

    @Column(name = "is_sepa", nullable = false)
    private boolean sepa;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CountryEntity(String countryCode, String alpha3Code, String countryName, String dialingCode,
                         String defaultCurrency, String ibanPattern, Integer ibanLength,
                         String swiftPrefix, boolean sepa) {
        this.countryCode = countryCode;
        this.alpha3Code = alpha3Code;
        this.countryName = countryName;
        this.dialingCode = dialingCode;
        this.defaultCurrency = defaultCurrency;
        this.ibanPattern = ibanPattern;
        this.ibanLength = ibanLength;
        this.swiftPrefix = swiftPrefix;
        this.sepa = sepa;
        this.createdAt = Instant.now();
    }
}
