package com.minimarket.pos.shared.infrastructure.security;

import com.minimarket.pos.shared.infrastructure.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemWriter {

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String title,
            String detail)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        Object correlation = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        String correlationId = correlation instanceof String value
                ? value
                : UUID.randomUUID().toString();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter()
                .write("""
                {"type":"urn:pos-minimarket:problem:%s","title":"%s","status":%d,"detail":"%s","code":"%s","correlationId":"%s"}
                """.formatted(code, title, status, detail, code, correlationId).strip());
    }
}
