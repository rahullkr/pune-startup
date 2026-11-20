package com.punestartup.auth.service;

import com.punestartup.auth.entity.AccountStatus;
import com.punestartup.auth.entity.AuthSession;
import com.punestartup.auth.entity.User;
import com.punestartup.auth.entity.UserRole;
import com.punestartup.auth.repository.AuthSessionRepository;
import com.punestartup.auth.repository.UserRoleRepository;
import com.punestartup.auth.security.CandidatePrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "psa_";

    private final AuthSessionRepository sessionRepository;
    private final UserRoleRepository userRoleRepository;
    private final Duration sessionTtl;

    public AuthSessionService(AuthSessionRepository sessionRepository,
                              UserRoleRepository userRoleRepository,
                              @Value("${app.auth.session-ttl}") Duration sessionTtl) {
        this.sessionRepository = sessionRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionTtl = sessionTtl;
    }

    @Transactional
    public IssuedSession createSession(User user, String userAgent) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(sessionTtl);

        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setTokenHash(hash(token));
        session.setUserAgent(truncate(userAgent, 255));
        session.setExpiresAt(expiresAt);
        sessionRepository.saveAndFlush(session);

        return new IssuedSession(token, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<CandidatePrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        return sessionRepository.findByTokenHash(hash(rawToken))
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> session.getExpiresAt().isAfter(now))
                .filter(session -> session.getUser().getAccountStatus() == AccountStatus.ACTIVE)
                .map(session -> {
                    List<UserRole> roles = userRoleRepository.findByUser_Id(session.getUser().getId());
                    return new CandidatePrincipal(
                            session.getUser().getId(),
                            session.getId(),
                            session.getUser().getEmail(),
                            roles.stream().map(role -> role.getId().getRole()).toList());
                });
    }

    @Transactional
    public void revoke(UUID sessionId, UUID userId) {
        sessionRepository.findById(sessionId)
                .filter(session -> session.getUser().getId().equals(userId))
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(Instant.now());
                        sessionRepository.saveAndFlush(session);
                    }
                });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        sessionRepository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public void revokeOtherSessions(UUID userId, UUID sessionId) {
        sessionRepository.revokeOtherSessions(userId, sessionId, Instant.now());
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
