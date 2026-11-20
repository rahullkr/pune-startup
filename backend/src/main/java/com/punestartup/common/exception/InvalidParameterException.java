package com.punestartup.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidParameterException extends ApiException {

    public InvalidParameterException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message);
    }

    public static InvalidParameterException of(String parameter, String reason) {
        return new InvalidParameterException("Invalid parameter '%s': %s".formatted(parameter, reason));
    }
}
