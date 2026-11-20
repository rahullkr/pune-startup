package com.punestartup.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.common.web.ErrorResponse;
import com.punestartup.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        RequestContext.setErrorCode(request, "UNAUTHORIZED");
        String requestId = RequestContext.resolve(request);
        ErrorResponse body = ErrorResponse.of(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                "Authentication is required", request.getRequestURI(), requestId);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
