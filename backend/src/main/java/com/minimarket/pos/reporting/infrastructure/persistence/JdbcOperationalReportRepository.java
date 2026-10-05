package com.minimarket.pos.reporting.infrastructure.persistence;

import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.reporting.application.OperationalReportRepository;
import com.minimarket.pos.reporting.domain.CashReport;
import com.minimarket.pos.reporting.domain.InventoryReport;
import com.minimarket.pos.reporting.domain.ReportPage;
import com.minimarket.pos.reporting.domain.SalesReport;
import com.minimarket.pos.sales.domain.PaymentMethod;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationalReportRepository implements OperationalReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOperationalReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SalesReport sales(
            LocalDate from,
            LocalDate to,
            Instant fromInstant,
            Instant toExclusive,
            PaymentMethod method) {
        List<Object> parameters = new ArrayList<>(List.of(
                Timestamp.from(fromInstant), Timestamp.from(toExclusive)));
        String methodFilter = "";
        if (method != null) {
            methodFilter = " AND payment.method = ?";
            parameters.add(method.name());
        }

        Map<String, Object> totals = jdbcTemplate.queryForMap(
                """
                SELECT COUNT(*) AS recorded_sales,
                       COALESCE(SUM(sale.total), 0) AS recorded_amount,
                       COUNT(*) FILTER (WHERE sale.status = 'VOIDED') AS voided_sales,
                       COALESCE(SUM(sale.total) FILTER (WHERE sale.status = 'VOIDED'), 0) AS voided_amount,
                       COUNT(*) FILTER (WHERE sale.status = 'CONFIRMED') AS net_sales,
                       COALESCE(SUM(sale.total) FILTER (WHERE sale.status = 'CONFIRMED'), 0) AS net_amount
                FROM sale
                JOIN payment ON payment.sale_id = sale.id
                WHERE sale.confirmed_at >= ? AND sale.confirmed_at < ?
                """ + methodFilter,
                parameters.toArray());

        BigDecimal profit = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(
                    (line.unit_sale_price - line.unit_estimated_cost) * line.quantity
                ), 0) AS estimated_profit
                FROM sale
                JOIN payment ON payment.sale_id = sale.id
                JOIN sale_line line ON line.sale_id = sale.id
                WHERE sale.confirmed_at >= ? AND sale.confirmed_at < ?
                  AND sale.status = 'CONFIRMED'
                """ + methodFilter,
                BigDecimal.class,
                parameters.toArray());

        List<SalesReport.PaymentSummary> payments = jdbcTemplate.query(
                """
                SELECT payment.method,
                       COUNT(*) AS recorded_sales,
                       COALESCE(SUM(sale.total), 0) AS recorded_amount,
                       COUNT(*) FILTER (WHERE sale.status = 'VOIDED') AS voided_sales,
                       COALESCE(SUM(sale.total) FILTER (WHERE sale.status = 'VOIDED'), 0) AS voided_amount,
                       COUNT(*) FILTER (WHERE sale.status = 'CONFIRMED') AS net_sales,
                       COALESCE(SUM(sale.total) FILTER (WHERE sale.status = 'CONFIRMED'), 0) AS net_amount
                FROM sale
                JOIN payment ON payment.sale_id = sale.id
                WHERE sale.confirmed_at >= ? AND sale.confirmed_at < ?
                """ + methodFilter + " GROUP BY payment.method ORDER BY payment.method",
                (rs, row) -> new SalesReport.PaymentSummary(
                        PaymentMethod.valueOf(rs.getString("method")),
                        rs.getLong("recorded_sales"),
                        rs.getBigDecimal("recorded_amount"),
                        rs.getLong("voided_sales"),
                        rs.getBigDecimal("voided_amount"),
                        rs.getLong("net_sales"),
                        rs.getBigDecimal("net_amount")),
                parameters.toArray());

        List<SalesReport.RefundSummary> refunds = jdbcTemplate.query(
                """
                SELECT cancellation.refund_method,
                       COUNT(*) AS cancellations,
                       COALESCE(SUM(cancellation.amount), 0) AS exact_amount,
                       COALESCE(SUM(COALESCE(cancellation.cash_payable, cancellation.amount)), 0)
                           AS payable_amount
                FROM sale_cancellation cancellation
                JOIN sale ON sale.id = cancellation.sale_id
                JOIN payment ON payment.sale_id = sale.id
                WHERE sale.confirmed_at >= ? AND sale.confirmed_at < ?
                """ + methodFilter + " GROUP BY cancellation.refund_method ORDER BY cancellation.refund_method",
                (rs, row) -> new SalesReport.RefundSummary(
                        PaymentMethod.valueOf(rs.getString("refund_method")),
                        rs.getLong("cancellations"),
                        rs.getBigDecimal("exact_amount"),
                        rs.getBigDecimal("payable_amount")),
                parameters.toArray());

        List<SalesReport.TopProduct> topProducts = jdbcTemplate.query(
                """
                SELECT line.product_id,
                       MAX(line.product_code) AS code,
                       MAX(line.product_name) AS name,
                       SUM(line.quantity) AS quantity,
                       SUM(line.subtotal) AS revenue
                FROM sale
                JOIN payment ON payment.sale_id = sale.id
                JOIN sale_line line ON line.sale_id = sale.id
                WHERE sale.confirmed_at >= ? AND sale.confirmed_at < ?
                  AND sale.status = 'CONFIRMED'
                """ + methodFilter
                        + " GROUP BY line.product_id ORDER BY quantity DESC, revenue DESC, name LIMIT 20",
                (rs, row) -> new SalesReport.TopProduct(
                        rs.getObject("product_id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getLong("quantity"),
                        rs.getBigDecimal("revenue")),
                parameters.toArray());

        return new SalesReport(
                from,
                to,
                method,
                number(totals, "recorded_sales"),
                decimal(totals, "recorded_amount"),
                number(totals, "voided_sales"),
                decimal(totals, "voided_amount"),
                number(totals, "net_sales"),
                decimal(totals, "net_amount"),
                profit == null ? BigDecimal.ZERO : profit,
                payments,
                refunds,
                topProducts);
    }

    @Override
    public InventoryReport inventory(
            LocalDate from,
            LocalDate to,
            Instant fromInstant,
            Instant toExclusive,
            InventoryMovementType type,
            UUID productId,
            int page,
            int size) {
        List<InventoryReport.LowStockProduct> lowStock = jdbcTemplate.query(
                """
                SELECT product.id, product.code, product.name, balance.quantity,
                       product.minimum_stock,
                       GREATEST(product.minimum_stock - balance.quantity, 0) AS shortage
                FROM catalog_product product
                JOIN inventory_balance balance ON balance.product_id = product.id
                WHERE product.status = 'ACTIVE' AND balance.quantity <= product.minimum_stock
                ORDER BY shortage DESC, product.name, product.id
                LIMIT 100
                """,
                (rs, row) -> new InventoryReport.LowStockProduct(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getInt("minimum_stock"),
                        rs.getInt("shortage")));

        StringBuilder filterSql =
                new StringBuilder(" WHERE movement.occurred_at >= ? AND movement.occurred_at < ?");
        List<Object> filterParameters = new ArrayList<>(
                List.of(Timestamp.from(fromInstant), Timestamp.from(toExclusive)));
        if (type != null) {
            filterSql.append(" AND movement.movement_type = ?");
            filterParameters.add(type.name());
        }
        if (productId != null) {
            filterSql.append(" AND movement.product_id = ?");
            filterParameters.add(productId);
        }
        List<InventoryReport.MovementSummary> summaries = jdbcTemplate.query(
                """
                SELECT movement.movement_type,
                       COUNT(*) AS movements,
                       SUM(movement.quantity) AS quantity,
                       SUM(movement.delta) AS net_delta
                FROM inventory_movement movement
                """ + filterSql
                        + " GROUP BY movement.movement_type ORDER BY movement.movement_type",
                (rs, row) -> new InventoryReport.MovementSummary(
                        InventoryMovementType.valueOf(rs.getString("movement_type")),
                        rs.getLong("movements"),
                        rs.getLong("quantity"),
                        rs.getLong("net_delta")),
                filterParameters.toArray());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement movement" + filterSql,
                Long.class,
                filterParameters.toArray());
        List<Object> pageParameters = new ArrayList<>(filterParameters);
        pageParameters.add(size);
        pageParameters.add((long) page * size);
        List<InventoryReport.Movement> movements = jdbcTemplate.query(
                """
                SELECT movement.id, movement.product_id, product.code, product.name,
                       movement.movement_type, movement.quantity, movement.delta,
                       movement.previous_balance, movement.resulting_balance,
                       movement.reason, movement.reference, actor.display_name, movement.occurred_at
                FROM inventory_movement movement
                JOIN catalog_product product ON product.id = movement.product_id
                JOIN app_user actor ON actor.id = movement.actor_id
                """ + filterSql
                        + " ORDER BY movement.occurred_at DESC, movement.id DESC LIMIT ? OFFSET ?",
                this::mapMovement,
                pageParameters.toArray());

        return new InventoryReport(
                from,
                to,
                type,
                productId,
                lowStock,
                summaries,
                new ReportPage<>(movements, page, size, total == null ? 0 : total));
    }

    @Override
    public CashReport cash(
            LocalDate from,
            LocalDate to,
            Instant fromInstant,
            Instant toExclusive,
            int page,
            int size) {
        Object[] parameters = {Timestamp.from(fromInstant), Timestamp.from(toExclusive)};
        Map<String, Object> totals = jdbcTemplate.queryForMap(
                """
                SELECT COUNT(*) AS opened_sessions,
                       COUNT(*) FILTER (WHERE status = 'CLOSED') AS closed_sessions,
                       COALESCE(SUM(opening_amount), 0) AS opening_amount,
                       COALESCE(SUM(expected_cash) FILTER (WHERE status = 'CLOSED'), 0) AS expected_cash,
                       COALESCE(SUM(counted_cash) FILTER (WHERE status = 'CLOSED'), 0) AS counted_cash,
                       COALESCE(SUM(cash_difference) FILTER (WHERE status = 'CLOSED'), 0) AS difference
                FROM cash_session
                WHERE opened_at >= ? AND opened_at < ?
                """,
                parameters);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cash_session WHERE opened_at >= ? AND opened_at < ?",
                Long.class,
                parameters);
        List<CashReport.Session> sessions = jdbcTemplate.query(
                """
                SELECT session.id, session.status, opener.display_name AS opened_by,
                       session.opened_at, session.opening_amount,
                       closer.display_name AS closed_by, session.closed_at,
                       session.expected_cash, session.counted_cash, session.cash_difference
                FROM cash_session session
                JOIN app_user opener ON opener.id = session.opened_by
                LEFT JOIN app_user closer ON closer.id = session.closed_by
                WHERE session.opened_at >= ? AND session.opened_at < ?
                ORDER BY session.opened_at DESC, session.id DESC LIMIT ? OFFSET ?
                """,
                this::mapCashSession,
                Timestamp.from(fromInstant),
                Timestamp.from(toExclusive),
                size,
                (long) page * size);
        return new CashReport(
                from,
                to,
                number(totals, "opened_sessions"),
                number(totals, "closed_sessions"),
                decimal(totals, "opening_amount"),
                decimal(totals, "expected_cash"),
                decimal(totals, "counted_cash"),
                decimal(totals, "difference"),
                new ReportPage<>(sessions, page, size, total == null ? 0 : total));
    }

    private InventoryReport.Movement mapMovement(ResultSet rs, int rowNumber) throws SQLException {
        return new InventoryReport.Movement(
                rs.getObject("id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                InventoryMovementType.valueOf(rs.getString("movement_type")),
                rs.getInt("quantity"),
                rs.getInt("delta"),
                rs.getInt("previous_balance"),
                rs.getInt("resulting_balance"),
                rs.getString("reason"),
                rs.getString("reference"),
                rs.getString("display_name"),
                rs.getTimestamp("occurred_at").toInstant());
    }

    private CashReport.Session mapCashSession(ResultSet rs, int rowNumber) throws SQLException {
        return new CashReport.Session(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getString("opened_by"),
                rs.getTimestamp("opened_at").toInstant(),
                rs.getBigDecimal("opening_amount"),
                rs.getString("closed_by"),
                instant(rs.getTimestamp("closed_at")),
                rs.getBigDecimal("expected_cash"),
                rs.getBigDecimal("counted_cash"),
                rs.getBigDecimal("cash_difference"));
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static long number(Map<String, Object> values, String key) {
        return ((Number) values.get(key)).longValue();
    }

    private static BigDecimal decimal(Map<String, Object> values, String key) {
        return (BigDecimal) values.get(key);
    }
}
