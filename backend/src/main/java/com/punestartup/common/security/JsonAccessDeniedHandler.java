package com.punestartup.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.common.web.ErrorResponse;
import com.punestartup.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RequestContext.setErrorCode(request, "FORBIDDEN");
        String requestId = RequestContext.resolve(request);
        ErrorResponse body = ErrorResponse.of(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "You do not have permission to access this resource", request.getRequestURI(), requestId);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
