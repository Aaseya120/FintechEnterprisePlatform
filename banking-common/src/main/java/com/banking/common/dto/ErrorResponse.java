package com.banking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "RFC 7807 compliant error details representation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "HTTP Status Code", example = "400")
        int status,

        @Schema(description = "Banking error code for client error mapping", example = "ERR_INSUFFICIENT_FUNDS")
        String errorCode,

        @Schema(description = "Human readable error description", example = "Account has insufficient available balance")
        String message,

        @Schema(description = "Path where error occurred", example = "/api/v1/payments/transfer")
        String path,

        @Schema(description = "Correlation / Trace ID for Distributed Log Tracing", example = "7a4f49b1-5a3d-4c31-9f93-01d81f2115ec")
        String correlationId,

        @Schema(description = "Field validation errors if applicable")
        List<ValidationError> validationErrors,

        @Schema(description = "Timestamp of error occurrence")
        Instant timestamp
) {
    public record ValidationError(String field, String rejectedValue, String reason) {}

    public static ErrorResponse of(int status, String errorCode, String message, String path, String correlationId) {
        return new ErrorResponse(status, errorCode, message, path, correlationId, null, Instant.now());
    }

    public static ErrorResponse validation(int status, String errorCode, String message, String path, String correlationId, List<ValidationError> errors) {
        return new ErrorResponse(status, errorCode, message, path, correlationId, errors, Instant.now());
    }
}
