package com.punestartup.job.repository;

import java.util.UUID;

public interface CompanyJobCountProjection {
    UUID getCompanyId();
    Long getJobCount();
}
