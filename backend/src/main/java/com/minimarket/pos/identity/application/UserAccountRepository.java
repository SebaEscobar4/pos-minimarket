package com.minimarket.pos.identity.application;

import com.minimarket.pos.identity.domain.UserAccount;
import java.util.Optional;

public interface UserAccountRepository {

    long countUsers();

    Optional<UserAccount> findByUsername(String username);

    void create(UserAccount account);

    void updatePassword(String username, String passwordHash, boolean passwordChangeRequired);
}
