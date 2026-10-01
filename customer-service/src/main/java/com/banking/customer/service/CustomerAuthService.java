package com.banking.customer.service;

import com.banking.common.security.JwtTokenProvider;
import com.banking.customer.domain.Customer;
import com.banking.customer.domain.CustomerCredential;
import com.banking.customer.dto.AuthDtos.*;
import com.banking.customer.repository.CustomerCredentialRepository;
import com.banking.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    private final CustomerCredentialRepository credentialRepository;
    private final CustomerRepository customerRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticates customer with username and password, returns signed JWT Access Token (15-min) and Refresh Token (7-day).
     */
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        CustomerCredential credential = credentialRepository.findByUsername(request.getUsername().trim().toLowerCase())
                .orElseThrow(() -> {
                    log.warn("Login failed: Username '{}' not found", request.getUsername());
                    return new IllegalArgumentException("Invalid username or password");
                });

        if (credential.isLocked()) {
            log.warn("Login rejected: Account '{}' is locked due to security policy", credential.getUsername());
            throw new IllegalStateException("Account is locked due to excessive failed attempts. Please contact bank support.");
        }

        if (!PASSWORD_ENCODER.matches(request.getPassword(), credential.getPasswordHash())) {
            int attempts = credential.getFailedLoginAttempts() + 1;
            credential.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                credential.setLocked(true);
                log.warn("SECURITY ALERT: Customer '{}' has been LOCKED after {} consecutive failed login attempts",
                        credential.getUsername(), attempts);
            }
            credential.setUpdatedAt(Instant.now());
            credentialRepository.save(credential);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Reset failed login attempts on successful authentication
        credential.setFailedLoginAttempts(0);
        credential.setLastLoginAt(Instant.now());

        // Generate tokens
        Customer customer = customerRepository.findById(credential.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found for credentials"));

        String tier = customer.getCustomerTier() != null ? customer.getCustomerTier().name() : "BASIC";
        List<String> roles = List.of("CUSTOMER", "TIER_" + tier);

        String accessToken = jwtTokenProvider.generateAccessToken(
                customer.getId(),
                customer.getCustomerNumber(),
                customer.getEmail(),
                tier,
                roles
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken();
        Instant refreshExpiry = Instant.now().plus(jwtTokenProvider.getRefreshTokenValidityDays(), ChronoUnit.DAYS);

        credential.setRefreshToken(refreshToken);
        credential.setRefreshTokenExpiry(refreshExpiry);
        credential.setUpdatedAt(Instant.now());
        credentialRepository.save(credential);

        log.info("Customer '{}' successfully logged in. JWT access token and refresh token issued.", credential.getUsername());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .mustChangePassword(credential.isMustChangePassword())
                .customerId(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .username(credential.getUsername())
                .email(customer.getEmail())
                .tier(tier)
                .issuedAt(Instant.now())
                .build();
    }

    /**
     * Refreshes JWT Access Token using an active Refresh Token.
     * Enforces Refresh Token Rotation (RTR) and detects Token Tampering & Token Replay attacks.
     */
    @Transactional
    public AuthResponseDto refreshAccessToken(RefreshTokenRequestDto request) {
        String presentedToken = request.getRefreshToken();

        // 1. Structure / Tamper check: Token must follow expected high-entropy format
        if (presentedToken == null || !presentedToken.startsWith("rt_") || presentedToken.length() < 35) {
            log.warn("SECURITY ALERT: Malformed or tampered refresh token rejected: {}", presentedToken);
            throw new IllegalArgumentException("Invalid or tampered refresh token format");
        }

        // 2. Active Token Lookup
        CustomerCredential credential = credentialRepository.findByRefreshToken(presentedToken)
                .orElseThrow(() -> {
                    // Tamper or Replay Attack: Presented token is either forged or has already been consumed!
                    log.error("CRITICAL SECURITY ALERT: Refresh token reuse or forgery detected! Presented token does not match any active session. " +
                            "This may indicate a stolen token replay attack.");
                    return new SecurityException("Invalid refresh token. Token may have been rotated or compromised. Please re-authenticate.");
                });

        // 3. Expiration Check
        if (credential.getRefreshTokenExpiry() != null && Instant.now().isAfter(credential.getRefreshTokenExpiry())) {
            log.warn("Refresh token expired for customer '{}'. Expiry: {}", credential.getUsername(), credential.getRefreshTokenExpiry());
            credential.setRefreshToken(null);
            credential.setRefreshTokenExpiry(null);
            credentialRepository.save(credential);
            throw new SecurityException("Refresh token has expired. Please login again.");
        }

        // 4. Token Rotation (RTR): Invalidate old refresh token, generate a brand new one
        Customer customer = customerRepository.findById(credential.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found"));

        String tier = customer.getCustomerTier() != null ? customer.getCustomerTier().name() : "BASIC";
        List<String> roles = List.of("CUSTOMER", "TIER_" + tier);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                customer.getId(),
                customer.getCustomerNumber(),
                customer.getEmail(),
                tier,
                roles
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        Instant newRefreshExpiry = Instant.now().plus(jwtTokenProvider.getRefreshTokenValidityDays(), ChronoUnit.DAYS);

        // Save rotated token in database
        credential.setRefreshToken(newRefreshToken);
        credential.setRefreshTokenExpiry(newRefreshExpiry);
        credential.setUpdatedAt(Instant.now());
        credentialRepository.save(credential);

        log.info("Refresh token rotated successfully for customer '{}'. New access token issued.", credential.getUsername());

        return AuthResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .mustChangePassword(credential.isMustChangePassword())
                .customerId(customer.getId())
                .customerNumber(customer.getCustomerNumber())
                .username(credential.getUsername())
                .email(customer.getEmail())
                .tier(tier)
                .issuedAt(Instant.now())
                .build();
    }

    /**
     * Securely invalidates refresh token upon user logout.
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            credentialRepository.findByRefreshToken(refreshToken).ifPresent(cred -> {
                cred.setRefreshToken(null);
                cred.setRefreshTokenExpiry(null);
                cred.setUpdatedAt(Instant.now());
                credentialRepository.save(cred);
                log.info("Customer '{}' logged out. Refresh token revoked.", cred.getUsername());
            });
        }
    }

    /**
     * Provisions initial login credentials upon compliance KYC onboarding approval.
     */
    @Transactional
    public InitialCredentialDto provisionInitialCredentials(Customer customer) {
        String username = (customer.getEmail() != null && !customer.getEmail().isBlank())
                ? customer.getEmail().toLowerCase()
                : "user_" + customer.getCustomerNumber().toLowerCase();

        // Generate strong 12-char temporary password: Alpha-numeric + special char
        String tempPassword = generateSecureTemporaryPassword();
        String passwordHash = PASSWORD_ENCODER.encode(tempPassword);

        CustomerCredential credential = credentialRepository.findByCustomerId(customer.getId())
                .orElse(new CustomerCredential(UUID.randomUUID().toString(), customer.getId(), username, passwordHash));

        credential.setUsername(username);
        credential.setPasswordHash(passwordHash);
        credential.setMustChangePassword(true);
        credential.setLocked(false);
        credential.setFailedLoginAttempts(0);
        credential.setUpdatedAt(Instant.now());

        credentialRepository.save(credential);
        log.info("Initial credentials generated for customer ID '{}' with username '{}'", customer.getId(), username);

        return InitialCredentialDto.builder()
                .username(username)
                .temporaryPassword(tempPassword)
                .mustChangePassword(true)
                .build();
    }

    /**
     * Allows customer to change their password and clears the mustChangePassword flag.
     */
    @Transactional
    public void changePassword(String customerId, ChangePasswordRequestDto request) {
        CustomerCredential credential = credentialRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Credentials not found for customer"));

        if (!PASSWORD_ENCODER.matches(request.getCurrentPassword(), credential.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        credential.setPasswordHash(PASSWORD_ENCODER.encode(request.getNewPassword()));
        credential.setMustChangePassword(false);
        credential.setUpdatedAt(Instant.now());
        credentialRepository.save(credential);
        log.info("Password successfully updated for customer ID '{}'", customerId);
    }

    private String generateSecureTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
