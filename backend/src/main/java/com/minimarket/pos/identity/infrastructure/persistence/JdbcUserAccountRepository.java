package com.minimarket.pos.identity.infrastructure.persistence;

import com.minimarket.pos.identity.application.UserAccountRepository;
import com.minimarket.pos.identity.domain.UserAccount;
import com.minimarket.pos.identity.domain.UserRole;
import com.minimarket.pos.identity.domain.UserStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccountRepository implements UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jdbcTemplate
                .query(
                        """
                        SELECT id, username, display_name, password_hash, role, status,
                               password_change_required
                        FROM app_user
                        WHERE username = ?
                        """,
                        this::mapAccount,
                        username)
                .stream()
                .findFirst();
    }

    @Override
    public void create(UserAccount account) {
        jdbcTemplate.update(
                """
                INSERT INTO app_user (
                    id, username, display_name, password_hash, role, status,
                    password_change_required
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                account.id(),
                account.username(),
                account.displayName(),
                account.passwordHash(),
                account.role().name(),
                account.status().name(),
                account.passwordChangeRequired());
    }

    @Override
    public void updatePassword(String username, String passwordHash, boolean passwordChangeRequired) {
        int updated = jdbcTemplate.update(
                """
                UPDATE app_user
                SET password_hash = ?, password_change_required = ?, updated_at = CURRENT_TIMESTAMP
                WHERE username = ?
                """,
                passwordHash,
                passwordChangeRequired,
                username);
        if (updated != 1) {
            throw new IllegalStateException("No se pudo actualizar la credencial del usuario.");
        }
    }

    private UserAccount mapAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserAccount(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                UserStatus.valueOf(resultSet.getString("status")),
                resultSet.getBoolean("password_change_required"));
    }
}
