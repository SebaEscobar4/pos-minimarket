package com.minimarket.pos.cash.infrastructure.persistence;

import com.minimarket.pos.cash.application.CashPage;
import com.minimarket.pos.cash.application.CashSessionRepository;
import com.minimarket.pos.cash.application.CashTotals;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementCategory;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.cash.domain.CashSessionStatus;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCashSessionRepository implements CashSessionRepository {

    private static final String OPEN_SESSION_QUERY = """
            SELECT session.id,
                   session.opened_by,
                   actor.display_name AS opened_by_display_name,
                   session.opened_at,
                   session.opening_amount,
                   session.status
            FROM cash_session session
            JOIN app_user actor ON actor.id = session.opened_by
            WHERE session.status = 'OPEN'
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCashSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CashSession> findOpen() {
        return jdbcTemplate.query(OPEN_SESSION_QUERY, this::mapSession).stream().findFirst();
    }

    @Override
    public Optional<CashSession> lockOpen() {
        return jdbcTemplate
                .query(OPEN_SESSION_QUERY + " FOR UPDATE OF session", this::mapSession)
                .stream()
                .findFirst();
    }

    @Override
    public boolean create(CashSession session) {
        return jdbcTemplate.update(
                        """
                        INSERT INTO cash_session (
                            id, opened_by, opened_at, opening_amount, status
                        ) VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (status) WHERE status = 'OPEN' DO NOTHING
                        """,
                        session.id(),
                        session.openedBy(),
                        Timestamp.from(session.openedAt()),
                        session.openingAmount().amount(),
                        session.status().name())
                == 1;
    }

    @Override
    public void append(CashMovement movement) {
        jdbcTemplate.update(
                """
                INSERT INTO cash_movement (
                    id, cash_session_id, movement_type, category, amount,
                    reason, reference, actor_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movement.id(),
                movement.cashSessionId(),
                movement.type().name(),
                movement.category() == null ? null : movement.category().name(),
                movement.amount().amount(),
                movement.reason(),
                movement.reference(),
                movement.actorId(),
                Timestamp.from(movement.occurredAt()));
    }

    @Override
    public CashTotals totals(UUID sessionId) {
        return jdbcTemplate.queryForObject(
                """
                WITH movement_totals AS (
                    SELECT COALESCE(SUM(amount) FILTER (WHERE movement_type = 'CASH_SALE'), 0) AS cash_sales,
                           COALESCE(SUM(amount) FILTER (WHERE movement_type = 'MANUAL_INCOME'), 0) AS manual_income,
                           COALESCE(SUM(amount) FILTER (WHERE movement_type = 'MANUAL_WITHDRAWAL'), 0) AS manual_withdrawals,
                           COALESCE(SUM(amount) FILTER (WHERE movement_type = 'CASH_REFUND'), 0) AS cash_refunds
                    FROM cash_movement
                    WHERE cash_session_id = ?
                ), payment_totals AS (
                    SELECT COALESCE(SUM(payment.amount) FILTER (WHERE payment.method = 'CARD'), 0) AS card_sales,
                           COALESCE(SUM(payment.amount) FILTER (WHERE payment.method = 'TRANSFER'), 0) AS transfer_sales
                    FROM payment
                    JOIN sale ON sale.id = payment.sale_id
                    WHERE sale.cash_session_id = ?
                )
                SELECT movement_totals.*, payment_totals.*
                FROM movement_totals CROSS JOIN payment_totals
                """,
                (resultSet, rowNumber) -> new CashTotals(
                        Money.of(resultSet.getBigDecimal("cash_sales")),
                        Money.of(resultSet.getBigDecimal("manual_income")),
                        Money.of(resultSet.getBigDecimal("manual_withdrawals")),
                        Money.of(resultSet.getBigDecimal("cash_refunds")),
                        Money.of(resultSet.getBigDecimal("card_sales")),
                        Money.of(resultSet.getBigDecimal("transfer_sales"))),
                sessionId,
                sessionId);
    }

    @Override
    public CashPage<CashMovement> movements(UUID sessionId, int page, int size) {
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cash_movement WHERE cash_session_id = ?",
                Long.class,
                sessionId);
        var items = jdbcTemplate.query(
                """
                SELECT movement.id,
                       movement.cash_session_id,
                       movement.movement_type,
                       movement.category,
                       movement.amount,
                       movement.reason,
                       movement.reference,
                       movement.actor_id,
                       actor.display_name AS actor_display_name,
                       movement.occurred_at
                FROM cash_movement movement
                JOIN app_user actor ON actor.id = movement.actor_id
                WHERE movement.cash_session_id = ?
                ORDER BY movement.occurred_at DESC, movement.id DESC
                LIMIT ? OFFSET ?
                """,
                this::mapMovement,
                sessionId,
                size,
                (long) page * size);
        return new CashPage<>(items, page, size, total);
    }

    @Override
    public boolean close(
            UUID sessionId,
            UUID actorId,
            Instant closedAt,
            Money countedCash,
            Money expectedCash,
            BigDecimal difference) {
        return jdbcTemplate.update(
                        """
                        UPDATE cash_session
                        SET status = 'CLOSED',
                            closed_by = ?,
                            closed_at = ?,
                            counted_cash = ?,
                            expected_cash = ?,
                            cash_difference = ?
                        WHERE id = ? AND status = 'OPEN'
                        """,
                        actorId,
                        Timestamp.from(closedAt),
                        countedCash.amount(),
                        expectedCash.amount(),
                        difference,
                        sessionId)
                == 1;
    }

    private CashSession mapSession(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CashSession(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("opened_by", UUID.class),
                resultSet.getString("opened_by_display_name"),
                resultSet.getTimestamp("opened_at").toInstant(),
                Money.of(resultSet.getBigDecimal("opening_amount")),
                CashSessionStatus.valueOf(resultSet.getString("status")));
    }

    private CashMovement mapMovement(ResultSet resultSet, int rowNumber) throws SQLException {
        String category = resultSet.getString("category");
        return new CashMovement(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("cash_session_id", UUID.class),
                CashMovementType.valueOf(resultSet.getString("movement_type")),
                category == null ? null : CashMovementCategory.valueOf(category),
                Money.of(resultSet.getBigDecimal("amount")),
                resultSet.getString("reason"),
                resultSet.getString("reference"),
                resultSet.getObject("actor_id", UUID.class),
                resultSet.getString("actor_display_name"),
                resultSet.getTimestamp("occurred_at").toInstant());
    }
}
