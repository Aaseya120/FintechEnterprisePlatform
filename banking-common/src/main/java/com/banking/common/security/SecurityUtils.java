package com.banking.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Security Context Utility:
 * Extracts authenticated user information (customerId, roles) from the Spring SecurityContext.
 * The SecurityContext is populated by JwtAuthenticationFilter in downstream services.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the authenticated customer ID (JWT subject).
     */
    public static Optional<String> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return Optional.ofNullable(auth.getName());
        }
        return Optional.empty();
    }

    /**
     * Returns the list of roles assigned to the current authenticated user.
     * Roles are in the format "ROLE_CUSTOMER", "ROLE_ADMIN", etc.
     */
    public static List<String> getCurrentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the current user has a specific role.
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }
}
