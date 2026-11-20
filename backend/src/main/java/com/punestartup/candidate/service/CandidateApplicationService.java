package com.punestartup.candidate.service;

import com.punestartup.candidate.dto.ApplicationDetailResponse;
import com.punestartup.candidate.dto.ApplicationStatusHistoryResponse;
import com.punestartup.candidate.dto.ApplicationSummaryResponse;
import com.punestartup.candidate.dto.CreateApplicationRequest;
import com.punestartup.candidate.dto.UpdateApplicationRequest;
import com.punestartup.candidate.entity.Application;
import com.punestartup.candidate.entity.ApplicationStatus;
import com.punestartup.candidate.entity.ApplicationStatusHistory;
import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.repository.ApplicationRepository;
import com.punestartup.candidate.repository.ApplicationStatusHistoryRepository;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.common.exception.ConflictException;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.web.PageRequests;
import com.punestartup.job.entity.Job;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.service.JobSummaryMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateApplicationService {

    private final CandidateProfileRepository profileRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final JobSummaryMapper summaryMapper;
    private final int maxPageSize;

    public CandidateApplicationService(CandidateProfileRepository profileRepository,
                                       ApplicationRepository applicationRepository,
                                       ApplicationStatusHistoryRepository historyRepository,
                                       JobRepository jobRepository,
                                       JobSummaryMapper summaryMapper,
                                       @Value("${app.search.max-page-size}") int maxPageSize) {
        this.profileRepository = profileRepository;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.jobRepository = jobRepository;
        this.summaryMapper = summaryMapper;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<ApplicationSummaryResponse> listApplications(UUID userId, ApplicationStatus status,
                                                              int page, int size) {
        CandidateProfile profile = findProfile(userId);
        PageRequest pageRequest = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("updatedAt")), maxPageSize);
        Page<Application> applications = status == null
                ? applicationRepository.findByCandidateProfile_Id(profile.getId(), pageRequest)
                : applicationRepository.findByCandidateProfile_IdAndStatus(profile.getId(), status, pageRequest);
        return applications.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplication(UUID userId, UUID applicationId) {
        CandidateProfile profile = findProfile(userId);
        Application application = findApplication(profile.getId(), applicationId);
        return toDetail(application);
    }

    @Transactional
    public ApplicationDetailResponse createApplication(UUID userId, CreateApplicationRequest request) {
        CandidateProfile profile = findProfile(userId);
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", request.jobId()));
        if (applicationRepository.existsByCandidateProfile_IdAndJob_Id(profile.getId(), job.getId())) {
            throw new ConflictException("An application already exists for this job");
        }

        ApplicationStatus status = request.status() == null ? ApplicationStatus.APPLIED : request.status();
        Instant appliedAt = request.appliedAt();
        if (appliedAt == null && status != ApplicationStatus.SAVED) {
            appliedAt = Instant.now();
        }

        Application application = new Application();
        application.setId(UUID.randomUUID());
        application.setCandidateProfile(profile);
        application.setJob(job);
        application.setStatus(status);
        application.setAppliedAt(appliedAt);
        application.setExternalApplicationUrl(validateUrl(request.externalApplicationUrl()));
        application.setNotes(trimToNull(request.notes()));
        try {
            applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An application already exists for this job");
        }

        addHistory(application, null, status, request.statusNote());
        return toDetail(application);
    }

    @Transactional
    public ApplicationDetailResponse updateApplication(UUID userId, UUID applicationId,
                                                       UpdateApplicationRequest request) {
        CandidateProfile profile = findProfile(userId);
        Application application = findApplication(profile.getId(), applicationId);
        ApplicationStatus oldStatus = application.getStatus();
        ApplicationStatus newStatus = request.status() == null ? oldStatus : request.status();

        if (Boolean.TRUE.equals(request.clearAppliedAt())) {
            application.setAppliedAt(null);
        } else if (request.appliedAt() != null) {
            application.setAppliedAt(request.appliedAt());
        }

        if (request.externalApplicationUrl() != null) {
            application.setExternalApplicationUrl(validateUrl(request.externalApplicationUrl()));
        }
        if (request.notes() != null) {
            application.setNotes(trimToNull(request.notes()));
        }

        application.setStatus(newStatus);
        if (newStatus != ApplicationStatus.SAVED && application.getAppliedAt() == null) {
            application.setAppliedAt(Instant.now());
        }
        applicationRepository.saveAndFlush(application);

        if (newStatus != oldStatus) {
            addHistory(application, oldStatus, newStatus, request.statusNote());
        }
        return toDetail(application);
    }

    @Transactional
    public void deleteApplication(UUID userId, UUID applicationId) {
        CandidateProfile profile = findProfile(userId);
        Application application = findApplication(profile.getId(), applicationId);
        historyRepository.deleteByApplication_Id(application.getId());
        applicationRepository.delete(application);
    }

    private CandidateProfile findProfile(UUID userId) {
        return profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", userId));
    }

    private Application findApplication(UUID profileId, UUID applicationId) {
        return applicationRepository.findByIdAndCandidateProfile_Id(applicationId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    private void addHistory(Application application, ApplicationStatus oldStatus, ApplicationStatus newStatus,
                            String note) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setId(UUID.randomUUID());
        history.setApplication(application);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNote(trimToNull(note));
        historyRepository.save(history);
    }

    private ApplicationSummaryResponse toSummary(Application application) {
        return ApplicationSummaryResponse.from(application, summaryMapper.toSummary(application.getJob()));
    }

    private ApplicationDetailResponse toDetail(Application application) {
        List<ApplicationStatusHistoryResponse> history =
                historyRepository.findByApplication_IdOrderByChangedAtAsc(application.getId()).stream()
                        .map(ApplicationStatusHistoryResponse::from)
                        .toList();
        return new ApplicationDetailResponse(
                application.getId(),
                application.getStatus().name(),
                application.getAppliedAt(),
                application.getExternalApplicationUrl(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                summaryMapper.toSummary(application.getJob()),
                history);
    }

    private String validateUrl(String value) {
        String url = trimToNull(value);
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute() || uri.getScheme() == null
                    || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
                throw InvalidParameterException.of("externalApplicationUrl", "must be an absolute http(s) URL");
            }
            return url;
        } catch (URISyntaxException ex) {
            throw InvalidParameterException.of("externalApplicationUrl", "must be a valid URL");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
