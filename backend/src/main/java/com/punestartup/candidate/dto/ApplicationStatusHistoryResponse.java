package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.ApplicationStatusHistory;
import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
        UUID id,
        String oldStatus,
        String newStatus,
        Instant changedAt,
        String note) {

    public static ApplicationStatusHistoryResponse from(ApplicationStatusHistory history) {
        return new ApplicationStatusHistoryResponse(
                history.getId(),
                history.getOldStatus() == null ? null : history.getOldStatus().name(),
                history.getNewStatus().name(),
                history.getChangedAt(),
                history.getNote());
    }
}
