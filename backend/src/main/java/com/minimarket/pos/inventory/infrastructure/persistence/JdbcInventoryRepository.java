package com.minimarket.pos.inventory.infrastructure.persistence;

import com.minimarket.pos.inventory.application.InventoryPage;
import com.minimarket.pos.inventory.application.InventoryRepository;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInventoryRepository implements InventoryRepository {

    private static final String SELECT_MOVEMENT = """
            SELECT m.id, m.product_id, m.movement_type, m.quantity, m.delta,
                   m.previous_balance, m.resulting_balance, m.reason, m.reference,
                   m.actor_id, u.display_name AS actor_display_name, m.occurred_at
            FROM inventory_movement m
            JOIN app_user u ON u.id = m.actor_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<LockedBalance> lockBalance(UUID productId) {
        return jdbcTemplate
                .query(
                        """
                        SELECT quantity, version
                        FROM inventory_balance
                        WHERE product_id = ?
                        FOR UPDATE
                        """,
                        (resultSet, rowNumber) -> new LockedBalance(
                                resultSet.getInt("quantity"), resultSet.getLong("version")),
                        productId)
                .stream()
                .findFirst();
    }

    @Override
    public boolean updateBalance(UUID productId, long expectedVersion, int resultingBalance) {
        return jdbcTemplate.update(
                        """
                        UPDATE inventory_balance
                        SET quantity = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE product_id = ? AND version = ?
                        """,
                        resultingBalance,
                        productId,
                        expectedVersion)
                == 1;
    }

    @Override
    public void append(InventoryMovement movement) {
        jdbcTemplate.update(
                """
                INSERT INTO inventory_movement (
                    id, product_id, movement_type, quantity, delta, previous_balance,
                    resulting_balance, reason, reference, actor_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movement.id(),
                movement.productId(),
                movement.type().name(),
                movement.quantity(),
                movement.delta(),
                movement.previousBalance(),
                movement.resultingBalance(),
                movement.reason(),
                movement.reference(),
                movement.actorId(),
                Timestamp.from(movement.occurredAt()));
    }

    @Override
    public Optional<BalanceView> findBalance(UUID productId) {
        return jdbcTemplate
                .query(
                        """
                        SELECT b.product_id, p.code AS product_code, p.name AS product_name,
                               b.quantity, b.version
                        FROM inventory_balance b
                        JOIN catalog_product p ON p.id = b.product_id
                        WHERE b.product_id = ?
                        """,
                        (resultSet, rowNumber) -> new BalanceView(
                                resultSet.getObject("product_id", UUID.class),
                                resultSet.getString("product_code"),
                                resultSet.getString("product_name"),
                                resultSet.getInt("quantity"),
                                resultSet.getLong("version")),
                        productId)
                .stream()
                .findFirst();
    }

    @Override
    public InventoryPage<InventoryMovement> findMovements(UUID productId, int page, int size) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE product_id = ?",
                Long.class,
                productId);
        List<InventoryMovement> movements = jdbcTemplate.query(
                SELECT_MOVEMENT
                        + " WHERE m.product_id = ?"
                        + " ORDER BY m.occurred_at DESC, m.id DESC LIMIT ? OFFSET ?",
                this::mapMovement,
                productId,
                size,
                (long) page * size);
        return new InventoryPage<>(movements, page, size, total == null ? 0 : total);
    }

    private InventoryMovement mapMovement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InventoryMovement(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("product_id", UUID.class),
                InventoryMovementType.valueOf(resultSet.getString("movement_type")),
                resultSet.getInt("quantity"),
                resultSet.getInt("delta"),
                resultSet.getInt("previous_balance"),
                resultSet.getInt("resulting_balance"),
                resultSet.getString("reason"),
                resultSet.getString("reference"),
                resultSet.getObject("actor_id", UUID.class),
                resultSet.getString("actor_display_name"),
                resultSet.getTimestamp("occurred_at").toInstant());
    }
}
