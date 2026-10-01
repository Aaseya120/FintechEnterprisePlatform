package com.banking.common.security;

/**
 * Centralized Role Constants:
 * Used across all microservices for consistent authorization checks with @PreAuthorize annotations.
 */
public final class BankingRoles {
    private BankingRoles() {}

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_TELLER = "ROLE_TELLER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_AUDITOR = "ROLE_AUDITOR";
}
