package com.minimarket.pos.identity.api;

import com.minimarket.pos.identity.application.AuthenticationAttemptLimiter;
import com.minimarket.pos.identity.application.AuthenticationAuditService;
import com.minimarket.pos.identity.application.AuthenticationAuditService.EventType;
import com.minimarket.pos.identity.application.IdentityPrincipal;
import com.minimarket.pos.identity.application.PasswordChangeService;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationAttemptLimiter attemptLimiter;
    private final AuthenticationAuditService auditService;
    private final PasswordChangeService passwordChangeService;

    public AuthenticationController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AuthenticationAttemptLimiter attemptLimiter,
            AuthenticationAuditService auditService,
            PasswordChangeService passwordChangeService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.attemptLimiter = attemptLimiter;
        this.auditService = auditService;
        this.passwordChangeService = passwordChangeService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @PostMapping("/login")
    public SessionResponse login(
            @RequestBody LoginRequest login,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sourceAddress = request.getRemoteAddr();
        if (attemptLimiter.isBlocked(login.username(), sourceAddress)) {
            auditService.record(EventType.LOGIN_RATE_LIMITED, login.username(), sourceAddress);
            throw new ApplicationException(ProblemType.RATE_LIMIT);
        }

        Authentication authenticated;
        if (login.username() == null || login.password() == null) {
            attemptLimiter.recordFailure(login.username(), sourceAddress);
            auditService.record(EventType.LOGIN_FAILED, login.username(), sourceAddress);
            throw new ApplicationException(ProblemType.AUTHENTICATION);
        }
        try {
            authenticated = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            login.username(), login.password()));
        } catch (AuthenticationException exception) {
            attemptLimiter.recordFailure(login.username(), sourceAddress);
            auditService.record(EventType.LOGIN_FAILED, login.username(), sourceAddress);
            throw new ApplicationException(ProblemType.AUTHENTICATION);
        }

        IdentityPrincipal principal = (IdentityPrincipal) authenticated.getPrincipal();
        attemptLimiter.recordSuccess(principal.getUsername());
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        auditService.record(EventType.LOGIN_SUCCEEDED, principal, sourceAddress);
        return SessionResponse.from(principal);
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        return SessionResponse.from((IdentityPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestBody ChangePasswordRequest change,
            Authentication authentication,
            HttpServletRequest request) {
        IdentityPrincipal principal = (IdentityPrincipal) authentication.getPrincipal();
        passwordChangeService.changePassword(
                principal, change.currentPassword(), change.newPassword());
        auditService.record(EventType.PASSWORD_CHANGED, principal, request.getRemoteAddr());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication, HttpServletRequest request) {
        IdentityPrincipal principal = (IdentityPrincipal) authentication.getPrincipal();
        auditService.record(EventType.LOGOUT_SUCCEEDED, principal, request.getRemoteAddr());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public record LoginRequest(String username, String password) {}

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    public record CsrfResponse(String headerName, String parameterName, String token) {}

    public record SessionResponse(
            String username,
            String displayName,
            String role,
            boolean passwordChangeRequired) {

        static SessionResponse from(IdentityPrincipal principal) {
            return new SessionResponse(
                    principal.getUsername(),
                    principal.displayName(),
                    principal.role(),
                    principal.passwordChangeRequired());
        }
    }
}
