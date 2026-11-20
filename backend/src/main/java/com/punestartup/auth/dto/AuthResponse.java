package com.punestartup.auth.dto;

import com.punestartup.candidate.dto.CandidateProfileResponse;
import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        CandidateProfileResponse candidate) {
}
