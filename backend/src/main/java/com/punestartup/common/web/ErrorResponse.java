package com.punestartup.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String errorCode, String message, String path, String requestId) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, requestId, null);
    }

    public static ErrorResponse withFieldErrors(int status, String errorCode, String message, String path,
                                                 String requestId, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, requestId, fieldErrors);
    }
}
