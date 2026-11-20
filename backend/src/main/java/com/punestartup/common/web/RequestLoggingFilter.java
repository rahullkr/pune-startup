package com.punestartup.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestContext.resolve(request);
        String previousRequestId = MDC.get(RequestContext.REQUEST_ID_MDC_KEY);
        MDC.put(RequestContext.REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(RequestContext.REQUEST_ID_HEADER, requestId);
        long startedAt = System.nanoTime();
        Exception failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            failure = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = failure == null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            String errorCode = RequestContext.errorCode(request);
            logRequest(request, status, durationMs, requestId, errorCode, failure);
            restoreMdc(previousRequestId);
        }
    }

    private void logRequest(HttpServletRequest request, int status, long durationMs, String requestId,
                            String errorCode, Exception failure) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String code = errorCode == null ? "-" : errorCode;
        if (status >= 500) {
            log.error("http_request method={} path={} status={} durationMs={} requestId={} errorCode={}",
                    method, path, status, durationMs, requestId, code, failure);
        } else if (status >= 400) {
            log.warn("http_request method={} path={} status={} durationMs={} requestId={} errorCode={}",
                    method, path, status, durationMs, requestId, code);
        } else {
            log.info("http_request method={} path={} status={} durationMs={} requestId={} errorCode={}",
                    method, path, status, durationMs, requestId, code);
        }
    }

    private void restoreMdc(String previousRequestId) {
        if (previousRequestId == null) {
            MDC.remove(RequestContext.REQUEST_ID_MDC_KEY);
        } else {
            MDC.put(RequestContext.REQUEST_ID_MDC_KEY, previousRequestId);
        }
    }
}
