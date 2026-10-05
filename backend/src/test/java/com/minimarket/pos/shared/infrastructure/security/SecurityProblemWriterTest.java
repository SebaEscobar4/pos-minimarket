package com.minimarket.pos.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.minimarket.pos.shared.infrastructure.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityProblemWriterTest {

    private final SecurityProblemWriter writer = new SecurityProblemWriter();

    @Test
    void writesAProblemResponseWithTheCurrentCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, "correlation-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, 401, "authentication-required", "Título", "Detalle");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("correlation-123", "authentication-required");
    }

    @Test
    void generatesACorrelationIdAndDoesNotRewriteACommittedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        writer.write(request, response, 403, "forbidden", "Acceso denegado", "Detalle");
        assertThat(response.getContentAsString()).containsPattern(
                "\\\"correlationId\\\":\\\"[0-9a-f-]{36}\\\"");

        response.setCommitted(true);
        String original = response.getContentAsString();
        writer.write(request, response, 500, "internal", "Error", "Detalle");
        assertThat(response.getContentAsString()).isEqualTo(original);
    }
}
