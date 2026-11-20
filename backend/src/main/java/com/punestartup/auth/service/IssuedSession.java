package com.punestartup.auth.service;

import java.time.Instant;

public record IssuedSession(String token, Instant expiresAt) {
}
