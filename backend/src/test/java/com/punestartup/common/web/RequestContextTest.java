package com.punestartup.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {

    @Test
    void preservesSafeClientRequestId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestContext.REQUEST_ID_HEADER, "web-a1b2c3.D4:E5_F6");

        assertThat(RequestContext.resolve(request)).isEqualTo("web-a1b2c3.D4:E5_F6");
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String requestId = RequestContext.resolve(request);

        assertThat(requestId).isNotBlank();
        assertThat(RequestContext.resolve(request)).isEqualTo(requestId);
    }

    @Test
    void replacesUnsafeRequestIdToPreventLogInjection() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestContext.REQUEST_ID_HEADER, "bad id\nwith log injection");

        assertThat(RequestContext.resolve(request))
                .isNotEqualTo("bad id\nwith log injection")
                .matches("^[0-9a-f-]{36}$");
    }

    @Test
    void storesErrorCodeOnTheRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestContext.setErrorCode(request, "VALIDATION_FAILED");

        assertThat(RequestContext.errorCode(request)).isEqualTo("VALIDATION_FAILED");
    }
}
