package com.punestartup.common.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
