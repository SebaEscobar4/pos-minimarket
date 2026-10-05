package com.minimarket.pos.identity.infrastructure.security;

import com.minimarket.pos.identity.application.IdentityPrincipal;
import com.minimarket.pos.identity.application.UserAccountRepository;
import com.minimarket.pos.identity.domain.IdentityInputRules;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final String GENERIC_FAILURE = "Usuario o contraseña inválidos.";

    private final UserAccountRepository repository;

    public DatabaseUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String candidate) throws UsernameNotFoundException {
        try {
            String username = IdentityInputRules.normalizeUsername(candidate);
            return repository
                    .findByUsername(username)
                    .map(IdentityPrincipal::new)
                    .orElseThrow(() -> new UsernameNotFoundException(GENERIC_FAILURE));
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException(GENERIC_FAILURE);
        }
    }
}
