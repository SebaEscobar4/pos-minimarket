package com.minimarket.pos.identity.application;

import com.minimarket.pos.identity.domain.IdentityInputRules;
import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserRole;
import com.minimarket.pos.identity.domain.UserStatus;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;

    public BootstrapAdminInitializer(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            IdentityProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.countUsers() > 0) {
            return;
        }

        IdentityProperties.Bootstrap bootstrap = properties.bootstrap();
        boolean usernameMissing = bootstrap.username() == null || bootstrap.username().isBlank();
        boolean passwordMissing = bootstrap.password() == null || bootstrap.password().isBlank();
        if (usernameMissing && passwordMissing) {
            LOGGER.warn(
                    "No existe un usuario interno y el bootstrap administrativo no está configurado. "
                            + "Define POS_BOOTSTRAP_ADMIN_USERNAME y POS_BOOTSTRAP_ADMIN_PASSWORD para crear el primero.");
            return;
        }
        if (usernameMissing || passwordMissing) {
            throw new IllegalStateException(
                    "El bootstrap requiere POS_BOOTSTRAP_ADMIN_USERNAME y POS_BOOTSTRAP_ADMIN_PASSWORD en conjunto.");
        }

        String username = IdentityInputRules.normalizeUsername(bootstrap.username());
        String displayName = IdentityInputRules.requireDisplayName(bootstrap.displayName());
        String password = IdentityInputRules.requirePassword(
                bootstrap.password(),
                properties.password().minimumLength(),
                properties.password().maximumLength());
        UserAccount administrator = new UserAccount(
                UUID.randomUUID(),
                username,
                displayName,
                passwordEncoder.encode(password),
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                true);
        repository.create(administrator);
        LOGGER.info(
                "Administrador inicial creado. Retira las variables de bootstrap y cambia la contraseña temporal al iniciar sesión.");
    }
}
