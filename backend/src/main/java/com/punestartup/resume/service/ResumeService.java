package com.punestartup.resume.service;

import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.common.exception.DependencyUnavailableException;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.resume.dto.ResumeResponse;
import com.punestartup.resume.entity.CandidateResume;
import com.punestartup.resume.entity.ResumeDeletedReason;
import com.punestartup.resume.entity.ResumeProcessingStatus;
import com.punestartup.resume.repository.CandidateResumeRepository;
import com.punestartup.resume.storage.ResumeStorage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private static final String STORAGE_PROVIDER = "R2";

    private final CandidateProfileRepository profileRepository;
    private final CandidateResumeRepository resumeRepository;
    private final ResumeStorage resumeStorage;
    private final ResumeFileValidator fileValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final long retentionDays;

    public ResumeService(CandidateProfileRepository profileRepository,
                         CandidateResumeRepository resumeRepository,
                         ResumeStorage resumeStorage,
                         ResumeFileValidator fileValidator,
                         ApplicationEventPublisher eventPublisher,
                         @Value("${app.resume.retention-days:30}") long retentionDays) {
        this.profileRepository = profileRepository;
        this.resumeRepository = resumeRepository;
        this.resumeStorage = resumeStorage;
        this.fileValidator = fileValidator;
        this.eventPublisher = eventPublisher;
        this.retentionDays = retentionDays;
    }

    @Transactional
    public ResumeResponse upload(UUID userId, MultipartFile file, boolean consent,
                                 String consentVersion, boolean primary) {
        if (!consent) {
            throw InvalidParameterException.of("consent", "must be true before uploading a resume");
        }
        String version = normalizeConsentVersion(consentVersion);
        CandidateProfile profile = findProfile(userId);
        ValidatedResumeFile validated = fileValidator.validate(file);

        UUID resumeId = UUID.randomUUID();
        String objectKey = "candidates/%s/resumes/%s.%s".formatted(
                profile.getId(), resumeId, validated.extension());
        String checksum = sha256(validated.content());
        try {
            resumeStorage.put(objectKey, new ByteArrayInputStream(validated.content()),
                    validated.content().length, validated.contentType(), checksum);
        } catch (RuntimeException ex) {
            log.warn("Could not store resume for candidate {}", profile.getId(), ex);
            throw new DependencyUnavailableException("Resume storage is unavailable");
        }

        boolean makePrimary = primary || resumeRepository
                .findByCandidateProfile_IdAndDeletedAtIsNull(profile.getId()).isEmpty();
        if (makePrimary) {
            clearPrimary(profile.getId());
        }

        Instant now = Instant.now();
        CandidateResume resume = new CandidateResume();
        resume.setId(resumeId);
        resume.setCandidateProfile(profile);
        resume.setStorageProvider(STORAGE_PROVIDER);
        resume.setStorageKey(objectKey);
        resume.setOriginalFilename(validated.originalFilename());
        resume.setContentType(validated.contentType());
        resume.setFileSizeBytes((long) validated.content().length);
        resume.setChecksumSha256(checksum);
        resume.setProcessingStatus(ResumeProcessingStatus.UPLOADED);
        resume.setPrimary(makePrimary);
        resume.setUploadedAt(now);
        resume.setConsentGivenAt(now);
        resume.setConsentVersion(version);
        resume.setRetentionExpiresAt(now.plus(Duration.ofDays(retentionDays)));
        resume.setParsedSkillCount(0);
        resume.setUpdatedAt(now);
        try {
            resume = resumeRepository.saveAndFlush(resume);
        } catch (RuntimeException ex) {
            try {
                resumeStorage.delete(objectKey);
            } catch (RuntimeException cleanupFailure) {
                log.warn("Could not remove orphaned resume object {}", objectKey, cleanupFailure);
            }
            throw ex;
        }

        eventPublisher.publishEvent(new ResumeUploadedEvent(resume.getId()));
        return toResponse(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(UUID userId) {
        CandidateProfile profile = findProfile(userId);
        return resumeRepository
                .findByCandidateProfile_IdAndDeletedAtIsNullAndRetentionExpiresAtAfterOrderByUploadedAtDesc(
                        profile.getId(), Instant.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(UUID userId, UUID resumeId) {
        CandidateResume resume = findActiveResume(userId, resumeId);
        return toResponse(resume);
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId) {
        CandidateResume resume = findActiveResume(userId, resumeId);
        deleteStoredObject(resume);
        UUID profileId = resume.getCandidateProfile().getId();
        boolean wasPrimary = resume.isPrimary();
        markDeleted(resume, ResumeDeletedReason.CANDIDATE_REQUEST);
        resumeRepository.saveAndFlush(resume);
        if (wasPrimary) {
            promoteNextPrimary(profileId);
        }
    }

    @Transactional
    public void deleteAllForCandidate(UUID candidateProfileId, ResumeDeletedReason reason) {
        List<CandidateResume> resumes =
                resumeRepository.findByCandidateProfile_IdAndDeletedAtIsNull(candidateProfileId);
        for (CandidateResume resume : resumes) {
            try {
                resumeStorage.delete(resume.getStorageKey());
            } catch (RuntimeException ex) {
                log.warn("Could not delete resume object for resume {}", resume.getId(), ex);
            }
            markDeleted(resume, reason);
            resumeRepository.save(resume);
        }
        resumeRepository.flush();
    }

    private CandidateResume findActiveResume(UUID userId, UUID resumeId) {
        CandidateResume resume = resumeRepository
                .findByIdAndCandidateProfile_User_IdAndDeletedAtIsNull(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        if (resume.isExpired(Instant.now())) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }
        return resume;
    }

    private CandidateProfile findProfile(UUID userId) {
        return profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", userId));
    }

    private void clearPrimary(UUID profileId) {
        List<CandidateResume> primaries = resumeRepository
                .findByCandidateProfile_IdAndDeletedAtIsNullAndPrimaryTrue(profileId);
        for (CandidateResume existing : primaries) {
            existing.setPrimary(false);
            resumeRepository.save(existing);
        }
        if (!primaries.isEmpty()) {
            resumeRepository.flush();
        }
    }

    private void promoteNextPrimary(UUID profileId) {
        resumeRepository
                .findByCandidateProfile_IdAndDeletedAtIsNullAndRetentionExpiresAtAfterOrderByUploadedAtDesc(
                        profileId, Instant.now())
                .stream()
                .filter(resume -> !resume.isPrimary())
                .findFirst()
                .ifPresent(next -> {
                    next.setPrimary(true);
                    resumeRepository.saveAndFlush(next);
                });
    }

    private void markDeleted(CandidateResume resume, ResumeDeletedReason reason) {
        resume.setDeletedAt(Instant.now());
        resume.setDeletedReason(reason);
        resume.setPrimary(false);
    }

    private void deleteStoredObject(CandidateResume resume) {
        try {
            resumeStorage.delete(resume.getStorageKey());
        } catch (RuntimeException ex) {
            log.warn("Could not delete resume object for resume {}", resume.getId(), ex);
            throw new DependencyUnavailableException("Resume storage is unavailable");
        }
    }

    private ResumeResponse toResponse(CandidateResume resume) {
        return ResumeResponse.from(resume, safePresignedUrl(resume));
    }

    private String safePresignedUrl(CandidateResume resume) {
        try {
            return resumeStorage.presignedGetUrl(resume.getStorageKey(), resume.getOriginalFilename());
        } catch (RuntimeException ex) {
            log.warn("Could not create download URL for resume {}", resume.getId(), ex);
            return null;
        }
    }

    private String normalizeConsentVersion(String consentVersion) {
        if (consentVersion == null || consentVersion.isBlank()) {
            throw InvalidParameterException.of("consentVersion", "must not be blank");
        }
        String value = consentVersion.trim();
        if (value.length() > 50) {
            throw InvalidParameterException.of("consentVersion", "must be at most 50 characters");
        }
        return value;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
