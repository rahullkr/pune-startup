package com.punestartup.resume.service;

import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.entity.CandidateSkill;
import com.punestartup.candidate.entity.CandidateSkillId;
import com.punestartup.candidate.entity.CandidateSkillSource;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import com.punestartup.resume.entity.CandidateResume;
import com.punestartup.resume.entity.ResumeProcessingStatus;
import com.punestartup.resume.repository.CandidateResumeRepository;
import com.punestartup.resume.storage.ResumeStorage;
import com.punestartup.skill.entity.Skill;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ResumeParsingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParsingService.class);

    private final CandidateResumeRepository resumeRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final ResumeStorage resumeStorage;
    private final ResumeTextExtractor textExtractor;
    private final ResumeSkillMatcher skillMatcher;
    private final TransactionTemplate transactionTemplate;

    public ResumeParsingService(CandidateResumeRepository resumeRepository,
                                CandidateSkillRepository candidateSkillRepository,
                                ResumeStorage resumeStorage,
                                ResumeTextExtractor textExtractor,
                                ResumeSkillMatcher skillMatcher,
                                TransactionTemplate transactionTemplate) {
        this.resumeRepository = resumeRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.resumeStorage = resumeStorage;
        this.textExtractor = textExtractor;
        this.skillMatcher = skillMatcher;
        this.transactionTemplate = transactionTemplate;
    }

    public void parse(UUID resumeId) {
        ResumeWork work = transactionTemplate.execute(status -> begin(resumeId));
        if (work == null) {
            return;
        }
        try (InputStream input = resumeStorage.get(work.storageKey())) {
            String text = textExtractor.extract(input, work.originalFilename());
            List<Skill> matchedSkills = skillMatcher.match(text);
            transactionTemplate.executeWithoutResult(status -> complete(resumeId, matchedSkills));
        } catch (ResumeParseException ex) {
            log.warn("Resume {} parsing failed: {}", resumeId, ex.getErrorCode());
            fail(resumeId, ex.getErrorCode());
        } catch (Exception ex) {
            log.warn("Resume {} parsing failed", resumeId, ex);
            fail(resumeId, "PARSE_FAILED");
        }
    }

    private ResumeWork begin(UUID resumeId) {
        CandidateResume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null || resume.isDeleted() || resume.isExpired(Instant.now())
                || resume.getProcessingStatus() == ResumeProcessingStatus.COMPLETED) {
            return null;
        }
        resume.setProcessingStatus(ResumeProcessingStatus.PROCESSING);
        resume.setParseErrorCode(null);
        resumeRepository.saveAndFlush(resume);
        return new ResumeWork(resume.getStorageKey(), resume.getOriginalFilename());
    }

    private void complete(UUID resumeId, List<Skill> matchedSkills) {
        CandidateResume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null || resume.isDeleted()) {
            return;
        }
        CandidateProfile profile = resume.getCandidateProfile();
        Map<UUID, CandidateSkill> existing = candidateSkillRepository
                .findByCandidateProfile_Id(profile.getId())
                .stream()
                .collect(Collectors.toMap(skill -> skill.getSkill().getId(), Function.identity(),
                        (first, second) -> first));
        for (Skill skill : matchedSkills) {
            if (!existing.containsKey(skill.getId())) {
                CandidateSkill candidateSkill = new CandidateSkill();
                candidateSkill.setId(new CandidateSkillId(profile.getId(), skill.getId()));
                candidateSkill.setCandidateProfile(profile);
                candidateSkill.setSkill(skill);
                candidateSkill.setSource(CandidateSkillSource.RESUME_PARSED);
                candidateSkillRepository.save(candidateSkill);
            }
        }
        resume.setProcessingStatus(ResumeProcessingStatus.COMPLETED);
        resume.setProcessedAt(Instant.now());
        resume.setParsedSkillCount(matchedSkills.size());
        resume.setParseErrorCode(null);
        resumeRepository.saveAndFlush(resume);
    }

    private void fail(UUID resumeId, String errorCode) {
        transactionTemplate.executeWithoutResult(status -> {
            CandidateResume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume == null || resume.isDeleted()) {
                return;
            }
            resume.setProcessingStatus(ResumeProcessingStatus.FAILED);
            resume.setProcessedAt(Instant.now());
            resume.setParseErrorCode(errorCode);
            resumeRepository.saveAndFlush(resume);
        });
    }

    private record ResumeWork(String storageKey, String originalFilename) {
    }
}
