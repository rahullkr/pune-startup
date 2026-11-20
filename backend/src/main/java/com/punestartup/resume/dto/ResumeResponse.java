package com.punestartup.resume.dto;

import com.punestartup.resume.entity.CandidateResume;
import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        String processingStatus,
        boolean primary,
        Instant uploadedAt,
        Instant processedAt,
        Instant consentGivenAt,
        String consentVersion,
        Instant retentionExpiresAt,
        int parsedSkillCount,
        String parseErrorCode,
        String downloadUrl) {

    public static ResumeResponse from(CandidateResume resume, String downloadUrl) {
        return new ResumeResponse(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getContentType(),
                resume.getFileSizeBytes(),
                resume.getProcessingStatus() == null ? null : resume.getProcessingStatus().name(),
                resume.isPrimary(),
                resume.getUploadedAt(),
                resume.getProcessedAt(),
                resume.getConsentGivenAt(),
                resume.getConsentVersion(),
                resume.getRetentionExpiresAt(),
                resume.getParsedSkillCount(),
                resume.getParseErrorCode(),
                downloadUrl);
    }
}
