package com.banking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard API response wrapper for banking operations")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "Success status flag", example = "true")
        boolean success,

        @Schema(description = "User or operational message", example = "Operation completed successfully")
        String message,

        @Schema(description = "Payload data")
        T data,

        @Schema(description = "Unique correlation / trace ID for tracking across microservices", example = "c8f2a1b9-7d84-4e4b-9231-50e4177c8e99")
        String correlationId,

        @Schema(description = "Timestamp of response generation")
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return new ApiResponse<>(true, message, data, correlationId, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, "SUCCESS", data, correlationId, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, String correlationId) {
        return new ApiResponse<>(false, message, null, correlationId, Instant.now());
    }
}
