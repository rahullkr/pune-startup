package com.punestartup.resume.service;

public record ValidatedResumeFile(
        byte[] content,
        String originalFilename,
        String contentType,
        String extension) {
}
