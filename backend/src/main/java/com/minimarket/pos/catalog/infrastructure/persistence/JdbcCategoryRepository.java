package com.minimarket.pos.catalog.infrastructure.persistence;

import com.minimarket.pos.catalog.application.CategoryRepository;
import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.CategoryStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCategoryRepository implements CategoryRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, name, status, created_at, updated_at
            FROM catalog_category
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(Category category, UUID actorId) {
        jdbcTemplate.update(
                """
                INSERT INTO catalog_category (
                    id, name, status, created_at, updated_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                category.id(),
                category.name(),
                category.status().name(),
                Timestamp.from(category.createdAt()),
                Timestamp.from(category.updatedAt()),
                actorId,
                actorId);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE id = ?", this::mapCategory, id).stream()
                .findFirst();
    }

    @Override
    public List<Category> findAll(boolean includeInactive) {
        if (includeInactive) {
            return jdbcTemplate.query(
                    SELECT_COLUMNS + " ORDER BY status, lower(name), id", this::mapCategory);
        }
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE status = 'ACTIVE' ORDER BY lower(name), id",
                this::mapCategory);
    }

    @Override
    public boolean updateName(UUID id, String name, UUID actorId) {
        return jdbcTemplate.update(
                        """
                        UPDATE catalog_category
                        SET name = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
                        WHERE id = ?
                        """,
                        name,
                        actorId,
                        id)
                == 1;
    }

    @Override
    public boolean updateStatus(UUID id, CategoryStatus status, UUID actorId) {
        return jdbcTemplate.update(
                        """
                        UPDATE catalog_category
                        SET status = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
                        WHERE id = ?
                        """,
                        status.name(),
                        actorId,
                        id)
                == 1;
    }

    private Category mapCategory(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Category(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                CategoryStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
