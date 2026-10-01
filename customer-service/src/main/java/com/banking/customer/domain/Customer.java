package com.banking.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customers_email_status", columnList = "email, status"),
        @Index(name = "idx_customers_phone", columnList = "phone")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_customers_num", columnNames = "customer_number"),
        @UniqueConstraint(name = "uk_customers_email", columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "customer_number", length = 20, nullable = false, unique = true)
    private String customerNumber;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", length = 20, nullable = false)
    private RiskCategory riskCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_tier", length = 20, nullable = false)
    private CustomerTier customerTier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum RiskCategory { LOW, MEDIUM, HIGH }
    public enum CustomerStatus { ONBOARDING, ACTIVE, SUSPENDED, TERMINATED }
    public enum CustomerTier {
        BASIC("Standard Banking", 5000.0, 3, false),
        PREMIUM("Priority Banking", 25000.0, 5, true),
        PLATINUM("Wealth & Concierge", 100000.0, 10, true),
        HNI("High Net-Worth Private Banking", 1000000.0, -1, true);

        private final String displayName;
        private final double dailyTransferLimit;
        private final int maxCards;
        private final boolean internationalAccess;

        CustomerTier(String displayName, double dailyTransferLimit, int maxCards, boolean internationalAccess) {
            this.displayName = displayName;
            this.dailyTransferLimit = dailyTransferLimit;
            this.maxCards = maxCards;
            this.internationalAccess = internationalAccess;
        }

        public String getDisplayName() { return displayName; }
        public double getDailyTransferLimit() { return dailyTransferLimit; }
        public int getMaxCards() { return maxCards; }
        public boolean isInternationalAccess() { return internationalAccess; }
    }

    public Customer(String id, String customerNumber, String firstName, String lastName,
                    String email, String phone, LocalDate dateOfBirth, String address) {
        this.id = id;
        this.customerNumber = customerNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.riskCategory = RiskCategory.LOW;
        this.status = CustomerStatus.ONBOARDING;
        this.customerTier = CustomerTier.BASIC;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void upgradeTier(CustomerTier newTier) {
        this.customerTier = newTier;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
}
