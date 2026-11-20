package com.punestartup.auth.repository;

import com.punestartup.auth.entity.AuthSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update AuthSession s
               set s.revokedAt = :now
             where s.user.id = :userId
               and s.revokedAt is null
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("""
            update AuthSession s
               set s.revokedAt = :now
             where s.user.id = :userId
               and s.id <> :sessionId
               and s.revokedAt is null
            """)
    int revokeOtherSessions(@Param("userId") UUID userId,
                            @Param("sessionId") UUID sessionId,
                            @Param("now") Instant now);
}
