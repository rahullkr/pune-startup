package com.punestartup.auth.controller;

import com.punestartup.auth.dto.AuthResponse;
import com.punestartup.auth.dto.ChangePasswordRequest;
import com.punestartup.auth.dto.LoginRequest;
import com.punestartup.auth.dto.RegisterRequest;
import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, userAgent));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.login(request, userAgent);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CandidatePrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
        return ResponseEntity.noContent().build();
    }
}
