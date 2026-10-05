package com.minimarket.pos.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SameOriginMutationFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;
    private final SecurityProblemWriter problemWriter;

    public SameOriginMutationFilter(
            @Value("${pos.security.allowed-origins}") List<String> allowedOrigins,
            SecurityProblemWriter problemWriter) {
        this.allowedOrigins = allowedOrigins.stream()
                .map(String::strip)
                .map(this::canonicalOrigin)
                .collect(Collectors.toUnmodifiableSet());
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return switch (request.getMethod()) {
            case "GET", "HEAD", "OPTIONS", "TRACE" -> true;
            default -> false;
        };
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String suppliedOrigin = request.getHeader("Origin");
        if (suppliedOrigin == null || suppliedOrigin.isBlank()) {
            suppliedOrigin = originFromReferer(request.getHeader("Referer"));
        }
        if (suppliedOrigin == null || allowedOrigins.contains(canonicalOrigin(suppliedOrigin))) {
            filterChain.doFilter(request, response);
            return;
        }
        problemWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN.value(),
                "invalid-origin",
                "Origen no permitido",
                "El origen de la solicitud no está autorizado.");
    }

    private String originFromReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        return canonicalOrigin(referer);
    }

    private String canonicalOrigin(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "<invalid>";
            }
            int port = uri.getPort();
            return uri.getScheme().toLowerCase()
                    + "://"
                    + uri.getHost().toLowerCase()
                    + (port < 0 ? "" : ":" + port);
        } catch (IllegalArgumentException exception) {
            return "<invalid>";
        }
    }
}
