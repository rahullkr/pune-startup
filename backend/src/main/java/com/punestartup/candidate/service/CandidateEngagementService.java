package com.punestartup.candidate.service;

import com.punestartup.candidate.dto.ApplicationSummaryResponse;
import com.punestartup.candidate.dto.CandidateDashboardResponse;
import com.punestartup.candidate.dto.FollowedCompanyResponse;
import com.punestartup.candidate.dto.SavedJobResponse;
import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.entity.FollowedCompany;
import com.punestartup.candidate.entity.FollowedCompanyId;
import com.punestartup.candidate.entity.SavedJob;
import com.punestartup.candidate.entity.SavedJobId;
import com.punestartup.candidate.repository.ApplicationRepository;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.FollowedCompanyRepository;
import com.punestartup.candidate.repository.SavedJobRepository;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.repository.CompanyJobCountProjection;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.service.JobSummaryMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateEngagementService {

    private static final int DASHBOARD_PREVIEW_SIZE = 5;

    private final CandidateProfileRepository profileRepository;
    private final SavedJobRepository savedJobRepository;
    private final FollowedCompanyRepository followedCompanyRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobSummaryMapper summaryMapper;
    private final int maxPageSize;

    public CandidateEngagementService(CandidateProfileRepository profileRepository,
                                      SavedJobRepository savedJobRepository,
                                      FollowedCompanyRepository followedCompanyRepository,
                                      ApplicationRepository applicationRepository,
                                      JobRepository jobRepository,
                                      CompanyRepository companyRepository,
                                      JobSummaryMapper summaryMapper,
                                      @Value("${app.search.max-page-size}") int maxPageSize) {
        this.profileRepository = profileRepository;
        this.savedJobRepository = savedJobRepository;
        this.followedCompanyRepository = followedCompanyRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.summaryMapper = summaryMapper;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<SavedJobResponse> listSavedJobs(UUID userId, int page, int size) {
        CandidateProfile profile = findProfile(userId);
        PageRequest pageRequest = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("savedAt")), maxPageSize);
        return savedJobRepository.findByCandidateProfile_Id(profile.getId(), pageRequest)
                .map(this::toSavedJob);
    }

    @Transactional(readOnly = true)
    public Optional<SavedJobResponse> findSavedJob(UUID userId, UUID jobId) {
        CandidateProfile profile = findProfile(userId);
        return savedJobRepository.findByCandidateProfile_IdAndJob_Id(profile.getId(), jobId)
                .map(this::toSavedJob);
    }

    @Transactional
    public SaveJobResult saveJob(UUID userId, UUID jobId) {
        CandidateProfile profile = findProfile(userId);
        var existing = savedJobRepository.findByCandidateProfile_IdAndJob_Id(profile.getId(), jobId);
        if (existing.isPresent()) {
            return new SaveJobResult(toSavedJob(existing.get()), false);
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        if (job.getStatus() != JobStatus.ACTIVE
                || job.getCompany() == null
                || job.getCompany().getRecordStatus() != RecordStatus.ACTIVE) {
            throw InvalidParameterException.of("jobId", "only active jobs from active companies can be saved");
        }

        SavedJob savedJob = new SavedJob();
        savedJob.setId(new SavedJobId(profile.getId(), jobId));
        savedJob.setCandidateProfile(profile);
        savedJob.setJob(job);
        savedJobRepository.saveAndFlush(savedJob);
        return new SaveJobResult(toSavedJob(savedJob), true);
    }

    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        CandidateProfile profile = findProfile(userId);
        savedJobRepository.findByCandidateProfile_IdAndJob_Id(profile.getId(), jobId)
                .ifPresent(savedJobRepository::delete);
    }

    @Transactional(readOnly = true)
    public Page<FollowedCompanyResponse> listFollowedCompanies(UUID userId, int page, int size) {
        CandidateProfile profile = findProfile(userId);
        PageRequest pageRequest = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("followedAt")), maxPageSize);
        Page<FollowedCompany> followed = followedCompanyRepository
                .findByCandidateProfile_Id(profile.getId(), pageRequest);
        Map<UUID, Long> counts = activeJobCounts(followed.getContent().stream()
                .map(entry -> entry.getCompany().getId())
                .toList());
        return followed.map(entry -> toFollowedCompany(entry, counts));
    }

    @Transactional(readOnly = true)
    public Optional<FollowedCompanyResponse> findFollowedCompany(UUID userId, UUID companyId) {
        CandidateProfile profile = findProfile(userId);
        return followedCompanyRepository.findByCandidateProfile_IdAndCompany_Id(profile.getId(), companyId)
                .map(entry -> toFollowedCompany(entry,
                        Map.of(companyId, activeJobCounts(List.of(companyId)).getOrDefault(companyId, 0L))));
    }

    @Transactional
    public FollowCompanyResult followCompany(UUID userId, UUID companyId) {
        CandidateProfile profile = findProfile(userId);
        var existing = followedCompanyRepository.findByCandidateProfile_IdAndCompany_Id(profile.getId(), companyId);
        if (existing.isPresent()) {
            long activeJobCount = activeJobCounts(List.of(companyId)).getOrDefault(companyId, 0L);
            return new FollowCompanyResult(toFollowedCompany(existing.get(), Map.of(companyId, activeJobCount)), false);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
        if (company.getRecordStatus() != RecordStatus.ACTIVE) {
            throw InvalidParameterException.of("companyId", "only active companies can be followed");
        }

        FollowedCompany followedCompany = new FollowedCompany();
        followedCompany.setId(new FollowedCompanyId(profile.getId(), companyId));
        followedCompany.setCandidateProfile(profile);
        followedCompany.setCompany(company);
        followedCompanyRepository.saveAndFlush(followedCompany);
        long activeJobCount = activeJobCounts(List.of(companyId)).getOrDefault(companyId, 0L);
        return new FollowCompanyResult(toFollowedCompany(followedCompany, Map.of(companyId, activeJobCount)), true);
    }

    @Transactional
    public void unfollowCompany(UUID userId, UUID companyId) {
        CandidateProfile profile = findProfile(userId);
        followedCompanyRepository.findByCandidateProfile_IdAndCompany_Id(profile.getId(), companyId)
                .ifPresent(followedCompanyRepository::delete);
    }

    @Transactional(readOnly = true)
    public CandidateDashboardResponse getDashboard(UUID userId) {
        CandidateProfile profile = findProfile(userId);
        PageRequest savedPreview = PageRequest.of(0, DASHBOARD_PREVIEW_SIZE,
                Sort.by(Sort.Order.desc("savedAt")));
        PageRequest followedPreview = PageRequest.of(0, DASHBOARD_PREVIEW_SIZE,
                Sort.by(Sort.Order.desc("followedAt")));
        PageRequest applicationPreview = PageRequest.of(0, DASHBOARD_PREVIEW_SIZE,
                Sort.by(Sort.Order.desc("updatedAt")));

        List<SavedJobResponse> savedJobs = savedJobRepository
                .findByCandidateProfile_Id(profile.getId(), savedPreview)
                .map(this::toSavedJob)
                .getContent();
        List<FollowedCompany> followed = followedCompanyRepository
                .findByCandidateProfile_Id(profile.getId(), followedPreview)
                .getContent();
        Map<UUID, Long> counts = activeJobCounts(followed.stream()
                .map(entry -> entry.getCompany().getId())
                .toList());
        List<FollowedCompanyResponse> followedCompanies = followed.stream()
                .map(entry -> toFollowedCompany(entry, counts))
                .toList();
        List<ApplicationSummaryResponse> applications = applicationRepository
                .findByCandidateProfile_Id(profile.getId(), applicationPreview)
                .map(application -> ApplicationSummaryResponse.from(
                        application, summaryMapper.toSummary(application.getJob())))
                .getContent();

        return new CandidateDashboardResponse(
                savedJobRepository.countByCandidateProfile_Id(profile.getId()),
                followedCompanyRepository.countByCandidateProfile_Id(profile.getId()),
                applicationRepository.countByCandidateProfile_Id(profile.getId()),
                savedJobs,
                followedCompanies,
                applications);
    }

    private CandidateProfile findProfile(UUID userId) {
        return profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", userId));
    }

    private SavedJobResponse toSavedJob(SavedJob savedJob) {
        Job job = savedJob.getJob();
        boolean available = job.getStatus() == JobStatus.ACTIVE
                && job.getCompany() != null
                && job.getCompany().getRecordStatus() == RecordStatus.ACTIVE;
        return new SavedJobResponse(job.getId(), savedJob.getSavedAt(), available, summaryMapper.toSummary(job));
    }

    private FollowedCompanyResponse toFollowedCompany(FollowedCompany followedCompany, Map<UUID, Long> counts) {
        Company company = followedCompany.getCompany();
        return new FollowedCompanyResponse(
                company.getId(),
                followedCompany.getFollowedAt(),
                company.getRecordStatus() == RecordStatus.ACTIVE,
                company.getName(),
                company.getSlug(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getVerificationStatus() == null ? null : company.getVerificationStatus().name(),
                company.getRecordStatus() == null ? null : company.getRecordStatus().name(),
                counts.getOrDefault(company.getId(), 0L));
    }

    private Map<UUID, Long> activeJobCounts(Collection<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Map.of();
        }
        return jobRepository.countActiveByCompanyIds(companyIds).stream()
                .collect(Collectors.toMap(CompanyJobCountProjection::getCompanyId,
                        CompanyJobCountProjection::getJobCount));
    }

    public record SaveJobResult(SavedJobResponse savedJob, boolean created) {
    }

    public record FollowCompanyResult(FollowedCompanyResponse company, boolean created) {
    }
}
