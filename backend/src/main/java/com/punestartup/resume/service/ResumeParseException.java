package com.punestartup.resume.service;

public class ResumeParseException extends RuntimeException {

    private final String errorCode;

    public ResumeParseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ResumeParseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
