package com.banking.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class AuthDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequestDto {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        private String deviceId;
        private String deviceFingerprint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequestDto {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;

        private String deviceId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequestDto {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        private String newPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponseDto {
        private String accessToken;
        private String refreshToken;
        private String tokenType; // "Bearer"
        private long expiresIn; // seconds
        private boolean mustChangePassword;
        private String customerId;
        private String customerNumber;
        private String username;
        private String email;
        private String tier;
        private Instant issuedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitialCredentialDto {
        private String username;
        private String temporaryPassword;
        private boolean mustChangePassword;
    }
}
