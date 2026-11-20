package com.punestartup.candidate.service;

import com.punestartup.candidate.dto.JobMatchResponse;
import com.punestartup.candidate.dto.MatchSignalResponse;
import com.punestartup.candidate.dto.MatchedSkillResponse;
import com.punestartup.candidate.dto.MissingSkillResponse;
import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.entity.CandidateSkill;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSkill;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.SkillRequirementType;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.service.JobSummaryMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateJobMatchService {

    private static final int REQUIRED_SKILL_WEIGHT = 2;
    private static final int PREFERRED_SKILL_WEIGHT = 1;

    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final JobRepository jobRepository;
    private final JobSummaryMapper summaryMapper;
    private final int maxPageSize;

    public CandidateJobMatchService(CandidateProfileRepository profileRepository,
                                    CandidateSkillRepository candidateSkillRepository,
                                    JobRepository jobRepository,
                                    JobSummaryMapper summaryMapper,
                                    @Value("${app.search.max-page-size}") int maxPageSize) {
        this.profileRepository = profileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.jobRepository = jobRepository;
        this.summaryMapper = summaryMapper;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<JobMatchResponse> matchJobs(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequests.of(page, size, null, maxPageSize);
        CandidateProfile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", userId));

        Map<UUID, CandidateSkill> candidateSkills = candidateSkillRepository
                .findByCandidateProfile_Id(profile.getId()).stream()
                .collect(Collectors.toMap(
                        candidateSkill -> candidateSkill.getSkill().getId(),
                        candidateSkill -> candidateSkill,
                        (first, second) -> first,
                        LinkedHashMap::new));
        if (candidateSkills.isEmpty()) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        List<JobMatchResponse> matches = jobRepository
                .findActiveJobsMatchingAnySkill(JobStatus.ACTIVE, RecordStatus.ACTIVE, candidateSkills.keySet())
                .stream()
                .map(job -> toMatch(job, candidateSkills))
                .sorted(matchComparator())
                .toList();

        int from = (int) Math.min(pageRequest.getOffset(), matches.size());
        int to = Math.min(from + pageRequest.getPageSize(), matches.size());
        return new PageImpl<>(matches.subList(from, to), pageRequest, matches.size());
    }

    private JobMatchResponse toMatch(Job job, Map<UUID, CandidateSkill> candidateSkills) {
        List<JobSkill> jobSkills = job.getSkills().stream()
                .filter(jobSkill -> jobSkill.getSkill() != null)
                .sorted(Comparator
                        .comparingInt((JobSkill jobSkill) -> weight(jobSkill) * -1)
                        .thenComparing(jobSkill -> jobSkill.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<JobSkill> requiredSkills = jobSkills.stream()
                .filter(jobSkill -> jobSkill.getRequirementType() == SkillRequirementType.REQUIRED)
                .toList();
        List<JobSkill> preferredSkills = jobSkills.stream()
                .filter(jobSkill -> jobSkill.getRequirementType() != SkillRequirementType.REQUIRED)
                .toList();

        List<JobSkill> matchedJobSkills = jobSkills.stream()
                .filter(jobSkill -> candidateSkills.containsKey(jobSkill.getSkill().getId()))
                .toList();
        List<JobSkill> missingJobSkills = jobSkills.stream()
                .filter(jobSkill -> !candidateSkills.containsKey(jobSkill.getSkill().getId()))
                .toList();

        int totalWeight = jobSkills.stream().mapToInt(this::weight).sum();
        int matchedWeight = matchedJobSkills.stream().mapToInt(this::weight).sum();
        int matchScore = totalWeight == 0 ? 0 : (int) Math.round(100.0 * matchedWeight / totalWeight);

        List<MatchedSkillResponse> matchedSkills = matchedJobSkills.stream()
                .map(jobSkill -> MatchedSkillResponse.from(
                        jobSkill, candidateSkills.get(jobSkill.getSkill().getId())))
                .toList();
        List<MissingSkillResponse> missingSkills = missingJobSkills.stream()
                .map(MissingSkillResponse::from)
                .toList();

        int matchedRequiredCount = countMatches(requiredSkills, candidateSkills);
        int matchedPreferredCount = countMatches(preferredSkills, candidateSkills);
        List<MatchSignalResponse> signals = signals(requiredSkills, preferredSkills, candidateSkills);

        return new JobMatchResponse(
                matchScore,
                matchedSkills.size(),
                requiredSkills.size(),
                matchedRequiredCount,
                preferredSkills.size(),
                matchedPreferredCount,
                summaryMapper.toSummary(job),
                matchedSkills,
                missingSkills,
                signals);
    }

    private List<MatchSignalResponse> signals(List<JobSkill> requiredSkills,
                                              List<JobSkill> preferredSkills,
                                              Map<UUID, CandidateSkill> candidateSkills) {
        List<MatchSignalResponse> signals = new ArrayList<>();
        addSignals(signals, "REQUIRED_SKILLS_MATCHED", "required", requiredSkills, candidateSkills, true);
        addSignals(signals, "REQUIRED_SKILLS_MISSING", "required", requiredSkills, candidateSkills, false);
        addSignals(signals, "PREFERRED_SKILLS_MATCHED", "preferred", preferredSkills, candidateSkills, true);
        addSignals(signals, "PREFERRED_SKILLS_MISSING", "preferred", preferredSkills, candidateSkills, false);
        return signals;
    }

    private void addSignals(List<MatchSignalResponse> signals,
                            String code,
                            String label,
                            List<JobSkill> jobSkills,
                            Map<UUID, CandidateSkill> candidateSkills,
                            boolean matched) {
        List<String> names = jobSkills.stream()
                .filter(jobSkill -> candidateSkills.containsKey(jobSkill.getSkill().getId()) == matched)
                .map(jobSkill -> jobSkill.getSkill().getName())
                .toList();
        if (names.isEmpty()) {
            return;
        }
        String verb = matched ? "Matches" : "Missing";
        String message = "%s %d of %d %s skills: %s".formatted(
                verb, names.size(), jobSkills.size(), label, String.join(", ", names));
        signals.add(new MatchSignalResponse(code, message));
    }

    private int countMatches(List<JobSkill> jobSkills, Map<UUID, CandidateSkill> candidateSkills) {
        return (int) jobSkills.stream()
                .filter(jobSkill -> candidateSkills.containsKey(jobSkill.getSkill().getId()))
                .count();
    }

    private int weight(JobSkill jobSkill) {
        return jobSkill.getRequirementType() == SkillRequirementType.REQUIRED
                ? REQUIRED_SKILL_WEIGHT
                : PREFERRED_SKILL_WEIGHT;
    }

    private Comparator<JobMatchResponse> matchComparator() {
        return Comparator
                .comparingInt(JobMatchResponse::matchScore).reversed()
                .thenComparing(Comparator.comparingInt(JobMatchResponse::matchedRequiredSkillCount).reversed())
                .thenComparing(Comparator.comparingInt(JobMatchResponse::matchedSkillCount).reversed())
                .thenComparing(match -> match.job().postedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(match -> match.job().id());
    }
}
