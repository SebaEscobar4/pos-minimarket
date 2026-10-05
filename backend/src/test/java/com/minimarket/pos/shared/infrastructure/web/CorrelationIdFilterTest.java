package com.minimarket.pos.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesValidUuidAndMakesItAvailableDuringRequest() throws Exception {
        String expected = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> valueInsideChain = new AtomicReference<>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, expected);

        filter.doFilter(
                request,
                response,
                (currentRequest, currentResponse) ->
                        valueInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)).isEqualTo(expected);
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(expected);
        assertThat(valueInsideChain).hasValue(expected);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesInvalidExternalValueWithGeneratedUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "invalid\nlog-value");

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {});

        String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(generated).isNotNull();
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
    }

    @Test
    void generatesUuidWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isNotBlank();
    }
}
