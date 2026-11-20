package com.punestartup.auth.service;

import com.punestartup.auth.dto.AuthResponse;
import com.punestartup.auth.dto.ChangePasswordRequest;
import com.punestartup.auth.dto.LoginRequest;
import com.punestartup.auth.dto.RegisterRequest;
import com.punestartup.auth.entity.AccountStatus;
import com.punestartup.auth.entity.User;
import com.punestartup.auth.entity.UserRole;
import com.punestartup.auth.entity.UserRoleId;
import com.punestartup.auth.entity.UserRoleName;
import com.punestartup.auth.repository.UserRepository;
import com.punestartup.auth.repository.UserRoleRepository;
import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.candidate.dto.CandidateProfileResponse;
import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.entity.CandidateSkill;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import com.punestartup.common.exception.ConflictException;
import com.punestartup.common.exception.ForbiddenException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.exception.UnauthorizedException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final AuthSessionService authSessionService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       CandidateProfileRepository candidateProfileRepository,
                       CandidateSkillRepository candidateSkillRepository,
                       AuthSessionService authSessionService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.authSessionService = authSessionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent) {
        String email = request.email().trim();
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByNormalizedEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setNormalizedEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        UserRole role = new UserRole();
        role.setId(new UserRoleId(user.getId(), UserRoleName.CANDIDATE));
        role.setUser(user);
        userRoleRepository.saveAndFlush(role);

        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        profile.setFullName(request.fullName().trim());
        profile = candidateProfileRepository.saveAndFlush(profile);

        IssuedSession issued = authSessionService.createSession(user, userAgent);
        return response(issued, profile, List.of());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent) {
        User user = userRepository.findByNormalizedEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("ACCOUNT_INACTIVE", "This account is not active");
        }

        CandidateProfile profile = candidateProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile", user.getId()));
        List<CandidateSkill> skills = candidateSkillRepository
                .findByCandidateProfile_IdOrderBySkill_NameAsc(profile.getId());
        IssuedSession issued = authSessionService.createSession(user, userAgent);
        return response(issued, profile, skills);
    }

    @Transactional
    public void logout(CandidatePrincipal principal) {
        authSessionService.revoke(principal.sessionId(), principal.userId());
    }

    @Transactional
    public void changePassword(CandidatePrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId()));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
        authSessionService.revokeOtherSessions(user.getId(), principal.sessionId());
    }

    private AuthResponse response(IssuedSession issued, CandidateProfile profile, List<CandidateSkill> skills) {
        return new AuthResponse(
                issued.token(),
                "Bearer",
                issued.expiresAt(),
                CandidateProfileResponse.from(profile, skills));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
