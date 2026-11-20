package com.punestartup.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "%s not found: %s".formatted(resourceType, String.valueOf(identifier)));
    }
}
