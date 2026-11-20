package com.punestartup.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.common.web.ErrorResponse;
import com.punestartup.common.web.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminApiKeyFilter extends OncePerRequestFilter {

    public static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private final ObjectMapper objectMapper;
    private final String expectedKey;

    public AdminApiKeyFilter(ObjectMapper objectMapper, String expectedKey) {
        this.objectMapper = objectMapper;
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isAdminPath(request.getRequestURI()) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(ADMIN_KEY_HEADER);
        if (!matches(providedKey)) {
            writeUnauthorized(request, response);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isAdminPath(String uri) {
        return uri.equals("/api/v1/admin") || uri.startsWith("/api/v1/admin/");
    }

    private boolean matches(String providedKey) {
        if (expectedKey == null || expectedKey.isBlank() || providedKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequestContext.setErrorCode(request, "UNAUTHORIZED");
        String requestId = RequestContext.resolve(request);
        ErrorResponse body = ErrorResponse.of(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                "A valid " + ADMIN_KEY_HEADER + " header is required", request.getRequestURI(), requestId);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
