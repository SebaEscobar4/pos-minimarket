package com.minimarket.pos.reporting.infrastructure.persistence;

import com.minimarket.pos.reporting.application.AuditEventPage;
import com.minimarket.pos.reporting.application.AuditEventRepository;
import com.minimarket.pos.reporting.domain.AuditEvent;
import com.minimarket.pos.reporting.domain.AuditEventType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditEventRepository implements AuditEventRepository {

    private static final String EVENTS = """
            WITH audit_events AS (
                SELECT 'AUTH-' || event.id::text AS event_id,
                       event.event_type AS event_type,
                       event.occurred_at,
                       event.user_id AS actor_id,
                       COALESCE(actor.display_name, event.username) AS actor_display_name,
                       event.username AS reference,
                       CASE event.event_type
                           WHEN 'LOGIN_SUCCEEDED' THEN 'Inicio de sesi\u00f3n exitoso'
                           WHEN 'LOGIN_FAILED' THEN 'Inicio de sesi\u00f3n rechazado'
                           WHEN 'LOGIN_RATE_LIMITED' THEN 'Inicio de sesi\u00f3n limitado por seguridad'
                           WHEN 'PASSWORD_CHANGED' THEN 'Contrase\u00f1a actualizada'
                           WHEN 'LOGOUT_SUCCEEDED' THEN 'Cierre de sesi\u00f3n exitoso'
                       END AS summary
                FROM authentication_audit_event event
                LEFT JOIN app_user actor ON actor.id = event.user_id

                UNION ALL

                SELECT 'CASH-OPEN-' || session.id::text,
                       'CASH_OPENED',
                       session.opened_at,
                       session.opened_by,
                       actor.display_name,
                       'CAJA-' || left(session.id::text, 8),
                       'Caja abierta con $' || session.opening_amount::text
                FROM cash_session session
                JOIN app_user actor ON actor.id = session.opened_by

                UNION ALL

                SELECT 'CASH-CLOSE-' || session.id::text,
                       'CASH_CLOSED',
                       session.closed_at,
                       session.closed_by,
                       actor.display_name,
                       'CAJA-' || left(session.id::text, 8),
                       'Caja cerrada: esperado $' || session.expected_cash::text
                           || ', contado $' || session.counted_cash::text
                           || ', diferencia $' || session.cash_difference::text
                FROM cash_session session
                JOIN app_user actor ON actor.id = session.closed_by
                WHERE session.status = 'CLOSED'

                UNION ALL

                SELECT 'CASH-MOVEMENT-' || movement.id::text,
                       movement.movement_type,
                       movement.occurred_at,
                       movement.actor_id,
                       actor.display_name,
                       COALESCE(movement.reference, 'CAJA-' || left(movement.cash_session_id::text, 8)),
                       CASE movement.movement_type
                           WHEN 'CASH_SALE' THEN 'Ingreso de efectivo por venta: $'
                           WHEN 'MANUAL_INCOME' THEN 'Ingreso manual de caja: $'
                           WHEN 'MANUAL_WITHDRAWAL' THEN 'Retiro manual de caja: $'
                           WHEN 'CASH_REFUND' THEN 'Devoluci\u00f3n en efectivo: $'
                       END || movement.amount::text
                FROM cash_movement movement
                JOIN app_user actor ON actor.id = movement.actor_id

                UNION ALL

                SELECT 'INVENTORY-' || movement.id::text,
                       'INVENTORY_' || movement.movement_type,
                       movement.occurred_at,
                       movement.actor_id,
                       actor.display_name,
                       COALESCE(movement.reference, product.code, product.name),
                       product.name || ': ' || CASE WHEN movement.delta > 0 THEN '+' ELSE '' END
                           || movement.delta::text || ' unidades, saldo '
                           || movement.previous_balance::text || ' a '
                           || movement.resulting_balance::text
                FROM inventory_movement movement
                JOIN app_user actor ON actor.id = movement.actor_id
                JOIN catalog_product product ON product.id = movement.product_id

                UNION ALL

                SELECT 'SALE-' || sale.id::text,
                       'SALE_CONFIRMED',
                       sale.confirmed_at,
                       sale.actor_id,
                       actor.display_name,
                       'V-' || lpad(sale.folio::text, 6, '0'),
                       'Venta confirmada por $' || sale.total::text
                FROM sale
                JOIN app_user actor ON actor.id = sale.actor_id

                UNION ALL

                SELECT 'SALE-VOID-' || cancellation.id::text,
                       'SALE_VOIDED',
                       cancellation.occurred_at,
                       cancellation.actor_id,
                       actor.display_name,
                       'V-' || lpad(sale.folio::text, 6, '0'),
                       'Venta anulada por $' || cancellation.amount::text
                           || '. Motivo: ' || cancellation.reason
                FROM sale_cancellation cancellation
                JOIN sale ON sale.id = cancellation.sale_id
                JOIN app_user actor ON actor.id = cancellation.actor_id
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AuditEventPage<AuditEvent> find(
            Instant from,
            Instant toExclusive,
            AuditEventType type,
            String reference,
            int page,
            int size) {
        StringBuilder filters = new StringBuilder(" WHERE 1 = 1");
        List<Object> filterParameters = new ArrayList<>();
        if (from != null) {
            filters.append(" AND occurred_at >= ?");
            filterParameters.add(Timestamp.from(from));
        }
        if (toExclusive != null) {
            filters.append(" AND occurred_at < ?");
            filterParameters.add(Timestamp.from(toExclusive));
        }
        if (type != null) {
            filters.append(" AND event_type = ?");
            filterParameters.add(type.name());
        }
        if (reference != null) {
            filters.append(" AND reference ILIKE ? ESCAPE '\\'");
            filterParameters.add("%" + escapeLike(reference) + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                EVENTS + "SELECT COUNT(*) FROM audit_events" + filters,
                Long.class,
                filterParameters.toArray());

        List<Object> pageParameters = new ArrayList<>(filterParameters);
        pageParameters.add(size);
        pageParameters.add((long) page * size);
        List<AuditEvent> events = jdbcTemplate.query(
                EVENTS
                        + "SELECT event_id, event_type, occurred_at, actor_id, actor_display_name, reference, summary "
                        + "FROM audit_events"
                        + filters
                        + " ORDER BY occurred_at DESC, event_id DESC LIMIT ? OFFSET ?",
                this::mapEvent,
                pageParameters.toArray());
        return new AuditEventPage<>(events, page, size, total == null ? 0 : total);
    }

    private AuditEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AuditEvent(
                resultSet.getString("event_id"),
                AuditEventType.valueOf(resultSet.getString("event_type")),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getObject("actor_id", UUID.class),
                resultSet.getString("actor_display_name"),
                resultSet.getString("reference"),
                resultSet.getString("summary"));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
