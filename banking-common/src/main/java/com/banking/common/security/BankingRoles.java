package com.banking.common.security;

public final class BankingRoles {
    private BankingRoles() {}

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_TELLER = "ROLE_TELLER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_AUDITOR = "ROLE_AUDITOR";

    // Keycloak Realm/Resource Scopes
    public static final String SCOPE_READ = "SCOPE_banking:read";
    public static final String SCOPE_WRITE = "SCOPE_banking:write";
    public static final String SCOPE_TRANSFER = "SCOPE_banking:transfer";
}
