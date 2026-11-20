package com.punestartup.ingestion.service;

class RecordValidationException extends RuntimeException {

    RecordValidationException(String message) {
        super(message);
    }
}
