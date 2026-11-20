package com.punestartup.auth.security;

import com.punestartup.auth.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class CandidateSessionFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;

    public CandidateSessionFilter(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            Authentication existing = SecurityContextHolder.getContext().getAuthentication();
            if (existing == null || existing instanceof AnonymousAuthenticationToken) {
                authSessionService.authenticate(token).ifPresent(principal -> {
                    List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .toList();
                    var authentication = UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
