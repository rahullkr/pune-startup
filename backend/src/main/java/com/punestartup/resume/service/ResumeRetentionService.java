package com.punestartup.resume.service;

import com.punestartup.resume.entity.CandidateResume;
import com.punestartup.resume.entity.ResumeDeletedReason;
import com.punestartup.resume.repository.CandidateResumeRepository;
import com.punestartup.resume.storage.ResumeStorage;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.resume", name = "cleanup-enabled",
        havingValue = "true", matchIfMissing = true)
public class ResumeRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeRetentionService.class);

    private final CandidateResumeRepository resumeRepository;
    private final ResumeStorage resumeStorage;

    public ResumeRetentionService(CandidateResumeRepository resumeRepository,
                                  ResumeStorage resumeStorage) {
        this.resumeRepository = resumeRepository;
        this.resumeStorage = resumeStorage;
    }

    @Scheduled(fixedDelayString = "${app.resume.cleanup-interval:PT1H}",
            initialDelayString = "${app.resume.cleanup-initial-delay:PT1M}")
    @Transactional
    public void cleanupExpired() {
        List<CandidateResume> expired = resumeRepository
                .findTop100ByDeletedAtIsNullAndRetentionExpiresAtLessThanEqualOrderByRetentionExpiresAtAsc(
                        Instant.now());
        Set<UUID> profilesNeedingPrimary = new HashSet<>();
        for (CandidateResume resume : expired) {
            try {
                resumeStorage.delete(resume.getStorageKey());
            } catch (RuntimeException ex) {
                log.warn("Could not delete expired resume object for resume {}", resume.getId(), ex);
                continue;
            }
            if (resume.isPrimary()) {
                profilesNeedingPrimary.add(resume.getCandidateProfile().getId());
            }
            resume.setDeletedAt(Instant.now());
            resume.setDeletedReason(ResumeDeletedReason.RETENTION_EXPIRED);
            resume.setPrimary(false);
            resumeRepository.save(resume);
        }
        resumeRepository.flush();
        for (UUID profileId : profilesNeedingPrimary) {
            promoteNextPrimary(profileId);
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
}
