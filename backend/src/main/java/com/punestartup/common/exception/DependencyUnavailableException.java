package com.punestartup.common.exception;

import org.springframework.http.HttpStatus;

public class DependencyUnavailableException extends ApiException {

    public DependencyUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", message);
    }
}
