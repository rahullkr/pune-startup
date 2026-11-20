package com.punestartup.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", message);
    }
}
