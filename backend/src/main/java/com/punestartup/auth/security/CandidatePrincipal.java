package com.punestartup.auth.security;

import com.punestartup.auth.entity.UserRoleName;
import java.util.List;
import java.util.UUID;

public record CandidatePrincipal(
        UUID userId,
        UUID sessionId,
        String email,
        List<UserRoleName> roles) {
}
