package com.minimarket.pos.identity.domain;

import java.util.UUID;

public record UserAccount(
        UUID id,
        String username,
        String displayName,
        String passwordHash,
        UserRole role,
        UserStatus status,
        boolean passwordChangeRequired) {}
