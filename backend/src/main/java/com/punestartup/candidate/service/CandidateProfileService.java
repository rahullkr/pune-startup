package com.punestartup.candidate.service;

import com.punestartup.auth.entity.AccountStatus;
import com.punestartup.auth.entity.User;
import com.punestartup.auth.repository.UserRepository;
import com.punestartup.auth.service.AuthSessionService;
import com.punestartup.candidate.dto.CandidateProfileResponse;
import com.punestartup.candidate.dto.CandidateSkillInput;
import com.punestartup.candidate.dto.UpdateCandidateProfileRequest;
import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.entity.CandidateSkill;
import com.punestartup.candidate.entity.CandidateSkillId;
import com.punestartup.candidate.entity.CandidateSkillSource;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.resume.entity.ResumeDeletedReason;
import com.punestartup.resume.service.ResumeService;
import com.punestartup.skill.entity.Skill;
import com.punestartup.skill.repository.SkillRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final ResumeService resumeService;

    public CandidateProfileService(CandidateProfileRepository profileRepository,
                                   CandidateSkillRepository candidateSkillRepository,
                                   SkillRepository skillRepository,
                                   UserRepository userRepository,
                                   AuthSessionService authSessionService,
                                   ResumeService resumeService) {
        this.profileRepository = profileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
        this.authSessionService = authSessionService;
        this.resumeService = resumeService;
    }

    @Transactional(readOnly = true)
    public CandidateProfileResponse getProfile(UUID userId) {
        return toResponse(findProfile(userId));
    }

    @Transactional
    public CandidateProfileResponse updateProfile(UUID userId, UpdateCandidateProfileRequest request) {
        CandidateProfile profile = findProfile(userId);
        if (request.fullName() != null) {
            profile.setFullName(requireText(request.fullName(), "fullName"));
        }
        if (request.headline() != null) {
            profile.setHeadline(trimToNull(request.headline()));
        }
        if (request.summary() != null) {
            profile.setSummary(trimToNull(request.summary()));
        }
        if (request.preferredCity() != null) {
            profile.setPreferredCity(trimToNull(request.preferredCity()));
        }
        if (Boolean.TRUE.equals(request.clearExperience())) {
            profile.setTotalExperienceMonths(null);
        } else if (request.totalExperienceMonths() != null) {
            if (request.totalExperienceMonths() < 0) {
                throw InvalidParameterException.of("totalExperienceMonths", "must be non-negative");
            }
            profile.setTotalExperienceMonths(request.totalExperienceMonths());
        }
        if (request.preferredWorkMode() != null) {
            if (request.preferredWorkMode() == WorkMode.UNKNOWN) {
                throw InvalidParameterException.of("preferredWorkMode",
                        "allowed values are 'ONSITE', 'HYBRID' and 'REMOTE'");
            }
            profile.setPreferredWorkMode(request.preferredWorkMode());
        }
        profile = profileRepository.saveAndFlush(profile);

        if (request.skills() != null) {
            replaceSkills(profile, request.skills());
        }
        return toResponse(profile);
    }

    @Transactional
    public void deactivate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        profileRepository.findByUser_Id(userId).ifPresent(profile ->
                resumeService.deleteAllForCandidate(profile.getId(), ResumeDeletedReason.ACCOUNT_DEACTIVATED));
        user.setAccountStatus(AccountStatus.DEACTIVATED);
        userRepository.saveAndFlush(user);
        authSessionService.revokeAll(userId);
    }

    private CandidateProfile findProfile(UUID userId) {
        return profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", userId));
    }

    private CandidateProfileResponse toResponse(CandidateProfile profile) {
        List<CandidateSkill> skills =
                candidateSkillRepository.findByCandidateProfile_IdOrderBySkill_NameAsc(profile.getId());
        return CandidateProfileResponse.from(profile, skills);
    }

    private void replaceSkills(CandidateProfile profile, List<CandidateSkillInput> inputs) {
        Map<UUID, CandidateSkill> existing = new LinkedHashMap<>();
        for (CandidateSkill candidateSkill : candidateSkillRepository.findByCandidateProfile_Id(profile.getId())) {
            existing.put(candidateSkill.getSkill().getId(), candidateSkill);
        }

        Map<UUID, CandidateSkillInput> desired = new LinkedHashMap<>();
        for (CandidateSkillInput input : inputs) {
            String name = requireText(input.name(), "skills.name");
            Skill skill = skillRepository.findByNormalizedName(TextNormalizer.normalizeName(name))
                    .orElseThrow(() -> new ResourceNotFoundException("Skill", name));
            desired.putIfAbsent(skill.getId(), input);
        }

        for (Map.Entry<UUID, CandidateSkillInput> entry : desired.entrySet()) {
            CandidateSkillInput input = entry.getValue();
            CandidateSkill current = existing.remove(entry.getKey());
            if (current == null) {
                CandidateSkill candidateSkill = new CandidateSkill();
                candidateSkill.setId(new CandidateSkillId(profile.getId(), entry.getKey()));
                candidateSkill.setCandidateProfile(profile);
                candidateSkill.setSkill(resolveSkill(entry.getKey()));
                applySkillDetails(candidateSkill, input);
                candidateSkillRepository.save(candidateSkill);
            } else {
                applySkillDetails(current, input);
                candidateSkillRepository.save(current);
            }
        }
        for (CandidateSkill leftover : existing.values()) {
            candidateSkillRepository.delete(leftover);
        }
    }

    private Skill resolveSkill(UUID skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", skillId));
    }

    private void applySkillDetails(CandidateSkill candidateSkill, CandidateSkillInput input) {
        candidateSkill.setProficiency(trimToNull(input.proficiency()));
        candidateSkill.setYearsExperience(input.yearsExperience());
        candidateSkill.setSource(CandidateSkillSource.SELF_DECLARED);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw InvalidParameterException.of(field, "must not be blank");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
