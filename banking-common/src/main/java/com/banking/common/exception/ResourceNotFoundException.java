package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BankingException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with identifier '%s' was not found", resourceType, identifier),
              HttpStatus.NOT_FOUND);
    }
}
