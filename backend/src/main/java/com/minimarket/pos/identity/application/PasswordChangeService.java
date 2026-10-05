package com.minimarket.pos.identity.application;

import com.minimarket.pos.identity.domain.IdentityInputRules;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public PasswordChangeService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            IdentityProperties properties,
            JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void changePassword(
            IdentityPrincipal principal, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, principal.getPassword())) {
            throw new ApplicationException(ProblemType.AUTHENTICATION);
        }
        String accepted;
        try {
            accepted = IdentityInputRules.requirePassword(
                    newPassword,
                    properties.password().minimumLength(),
                    properties.password().maximumLength());
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ProblemType.VALIDATION, exception.getMessage());
        }
        if (passwordEncoder.matches(accepted, principal.getPassword())) {
            throw new ApplicationException(
                    ProblemType.VALIDATION, "La contraseña nueva debe ser distinta de la actual.");
        }
        repository.updatePassword(principal.getUsername(), passwordEncoder.encode(accepted), false);
        jdbcTemplate.update(
                "DELETE FROM spring_session WHERE principal_name = ?", principal.getUsername());
    }
}
