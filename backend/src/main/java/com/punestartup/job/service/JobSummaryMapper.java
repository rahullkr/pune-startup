package com.punestartup.job.service;

import com.punestartup.company.dto.CompanyLocationResponse;
import com.punestartup.job.dto.JobCompanyResponse;
import com.punestartup.job.dto.JobSummaryResponse;
import com.punestartup.job.entity.Job;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JobSummaryMapper {

    private final BigDecimal fresherMaxExperienceYears;

    public JobSummaryMapper(@Value("${app.jobs.fresher-max-experience-years}") BigDecimal fresherMaxExperienceYears) {
        this.fresherMaxExperienceYears = fresherMaxExperienceYears;
    }

    public JobSummaryResponse toSummary(Job job) {
        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getEmploymentType() == null ? null : job.getEmploymentType().name(),
                job.getWorkMode() == null ? null : job.getWorkMode().name(),
                job.getExperienceMinYears(),
                job.getExperienceMaxYears(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getSalaryPeriod(),
                job.getStatus() == null ? null : job.getStatus().name(),
                postedAt(job),
                job.isFresherEligible(fresherMaxExperienceYears),
                job.getCompany() == null ? null : JobCompanyResponse.from(job.getCompany()),
                job.getCompanyLocation() == null ? null : CompanyLocationResponse.from(job.getCompanyLocation()));
    }

    public Instant postedAt(Job job) {
        return job.getFirstPublishedAt() != null ? job.getFirstPublishedAt() : job.getCreatedAt();
    }
}
