package com.punestartup.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

public final class RequestContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String REQUEST_ID_ATTRIBUTE = RequestContext.class.getName() + ".requestId";
    private static final String ERROR_CODE_ATTRIBUTE = RequestContext.class.getName() + ".errorCode";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$");

    private RequestContext() {
    }

    public static String resolve(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String requestId) {
            return requestId;
        }
        String requestId = safeOrGenerated(request.getHeader(REQUEST_ID_HEADER));
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }

    public static void setErrorCode(HttpServletRequest request, String errorCode) {
        request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode);
    }

    public static String errorCode(HttpServletRequest request) {
        Object value = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        return value instanceof String errorCode ? errorCode : null;
    }

    static String safeOrGenerated(String candidate) {
        if (candidate == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = candidate.trim();
        return SAFE_REQUEST_ID.matcher(trimmed).matches() ? trimmed : UUID.randomUUID().toString();
    }

    public static String currentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }
}
