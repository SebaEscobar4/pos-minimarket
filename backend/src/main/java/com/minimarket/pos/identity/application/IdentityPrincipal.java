package com.minimarket.pos.identity.application;

import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class IdentityPrincipal implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final String role;
    private final boolean active;
    private final boolean passwordChangeRequired;

    public IdentityPrincipal(UserAccount account) {
        this.id = account.id();
        this.username = account.username();
        this.displayName = account.displayName();
        this.passwordHash = account.passwordHash();
        this.role = account.role().name();
        this.active = account.status() == UserStatus.ACTIVE;
        this.passwordChangeRequired = account.passwordChangeRequired();
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String role() {
        return role;
    }

    public boolean passwordChangeRequired() {
        return passwordChangeRequired;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
