package com.minimarket.pos.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.minimarket.pos.shared.infrastructure.security.SecurityProblemWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SameOriginMutationFilterTest {

    private final SameOriginMutationFilter filter = new SameOriginMutationFilter(
            List.of("http://localhost:5173", "http://127.0.0.1:5173", "HTTPS://POS.EXAMPLE:443/path"),
            new SecurityProblemWriter());

    @Test
    void permitsSafeRequestsAndMutationsWithoutBrowserOriginHeaders() throws Exception {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/auth/session");
        MockFilterChain getChain = new MockFilterChain();
        filter.doFilter(get, new MockHttpServletResponse(), getChain);
        assertThat(getChain.getRequest()).isNotNull();

        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockFilterChain postChain = new MockFilterChain();
        filter.doFilter(post, new MockHttpServletResponse(), postChain);
        assertThat(postChain.getRequest()).isNotNull();
    }

    @Test
    void permitsAnAllowedOriginOrReferer() throws Exception {
        MockHttpServletRequest origin = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        origin.addHeader("Origin", "http://LOCALHOST:5173");
        MockFilterChain originChain = new MockFilterChain();
        filter.doFilter(origin, new MockHttpServletResponse(), originChain);
        assertThat(originChain.getRequest()).isNotNull();

        MockHttpServletRequest loopbackOrigin = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        loopbackOrigin.addHeader("Origin", "http://127.0.0.1:5173");
        MockFilterChain loopbackOriginChain = new MockFilterChain();
        filter.doFilter(loopbackOrigin, new MockHttpServletResponse(), loopbackOriginChain);
        assertThat(loopbackOriginChain.getRequest()).isNotNull();

        MockHttpServletRequest referer = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        referer.addHeader("Referer", "https://pos.example:443/login");
        MockFilterChain refererChain = new MockFilterChain();
        filter.doFilter(referer, new MockHttpServletResponse(), refererChain);
        assertThat(refererChain.getRequest()).isNotNull();
    }

    @Test
    void rejectsForeignOrMalformedBrowserOrigins() throws Exception {
        MockHttpServletRequest foreign = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        foreign.addHeader("Origin", "https://attacker.example");
        MockHttpServletResponse foreignResponse = new MockHttpServletResponse();
        filter.doFilter(foreign, foreignResponse, new MockFilterChain());
        assertThat(foreignResponse.getStatus()).isEqualTo(403);
        assertThat(foreignResponse.getContentAsString()).contains("invalid-origin");

        MockHttpServletRequest malformed = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        malformed.addHeader("Referer", ":not-a-uri");
        MockHttpServletResponse malformedResponse = new MockHttpServletResponse();
        filter.doFilter(malformed, malformedResponse, new MockFilterChain());
        assertThat(malformedResponse.getStatus()).isEqualTo(403);
    }
}
