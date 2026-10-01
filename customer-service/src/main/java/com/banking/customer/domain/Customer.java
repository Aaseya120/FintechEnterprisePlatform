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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum RiskCategory { LOW, MEDIUM, HIGH }
    public enum CustomerStatus { ONBOARDING, ACTIVE, SUSPENDED, TERMINATED }

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
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
}
