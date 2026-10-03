package com.banking.common.exception;

import com.banking.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
    }

    @ExceptionHandler(BankingException.class)
    @SuppressWarnings("null")
    public ResponseEntity<ErrorResponse> handleBankingException(BankingException ex, HttpServletRequest request) {
        String correlationId = getOrGenerateCorrelationId(request);
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        log.warn("Banking business exception occurred [code: {}, path: {}, correlationId: {}]: {}",
                ex.getErrorCode(), request.getRequestURI(), correlationId, ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                status.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = getOrGenerateCorrelationId(request);
        log.warn("Validation failure [path: {}, correlationId: {}]: {}", request.getRequestURI(), correlationId, ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ErrorResponse.ValidationError(
                    fieldError.getField(),
                    String.valueOf(fieldError.getRejectedValue()),
                    fieldError.getDefaultMessage()
            ));
        }

        ErrorResponse response = ErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation constraints were violated",
                request.getRequestURI(),
                correlationId,
                validationErrors
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String correlationId = getOrGenerateCorrelationId(request);
        log.warn("Access denied [path: {}, correlationId: {}]: {}", request.getRequestURI(), correlationId, ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access is denied: insufficient role privileges",
                request.getRequestURI(),
                correlationId
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        String correlationId = getOrGenerateCorrelationId(request);
        log.error("Unhandled internal server error [path: {}, correlationId: {}]", request.getRequestURI(), correlationId, ex);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred while processing banking transaction",
                request.getRequestURI(),
                correlationId
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
