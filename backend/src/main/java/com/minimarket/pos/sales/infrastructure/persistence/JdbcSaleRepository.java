package com.minimarket.pos.sales.infrastructure.persistence;

import com.minimarket.pos.sales.application.SalePage;
import com.minimarket.pos.sales.application.SaleRepository;
import com.minimarket.pos.sales.domain.Payment;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleCancellation;
import com.minimarket.pos.sales.domain.SaleLine;
import com.minimarket.pos.sales.domain.SaleStatus;
import com.minimarket.pos.shared.domain.Money;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSaleRepository implements SaleRepository {

    private static final String SELECT_SALE = """
            SELECT sale.id, sale.folio, sale.cash_session_id, sale.actor_id,
                   actor.display_name AS actor_display_name, sale.confirmed_at,
                   sale.status, sale.total, sale.idempotency_key, sale.request_hash
            FROM sale
            JOIN app_user actor ON actor.id = sale.actor_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSaleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void lockIdempotencyKey(UUID idempotencyKey) {
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))",
                resultSet -> {
                    resultSet.next();
                    return null;
                },
                idempotencyKey);
    }

    @Override
    public Optional<Sale> findByIdempotencyKey(UUID idempotencyKey) {
        return findOne(SELECT_SALE + " WHERE sale.idempotency_key = ?", idempotencyKey);
    }

    @Override
    public Optional<Sale> findById(UUID id) {
        return findOne(SELECT_SALE + " WHERE sale.id = ?", id);
    }

    @Override
    public Optional<Sale> findByIdForUpdate(UUID id) {
        return findOne(SELECT_SALE + " WHERE sale.id = ? FOR UPDATE OF sale", id);
    }

    @Override
    public long nextFolio() {
        Long folio = jdbcTemplate.queryForObject(
                "SELECT nextval('sale_folio_sequence')", Long.class);
        if (folio == null) {
            throw new IllegalStateException("sale folio sequence returned null");
        }
        return folio;
    }

    @Override
    public void save(Sale sale) {
        jdbcTemplate.update(
                """
                INSERT INTO sale (
                    id, folio, cash_session_id, actor_id, confirmed_at, status,
                    total, idempotency_key, request_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sale.id(),
                sale.folio(),
                sale.cashSessionId(),
                sale.actorId(),
                Timestamp.from(sale.confirmedAt()),
                sale.status().name(),
                sale.total().amount(),
                sale.idempotencyKey(),
                sale.requestHash());

        int lineNumber = 1;
        for (SaleLine line : sale.lines()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO sale_line (
                        id, sale_id, line_number, product_id, product_name, product_code,
                        quantity, unit_sale_price, unit_estimated_cost, subtotal
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    line.id(),
                    sale.id(),
                    lineNumber++,
                    line.productId(),
                    line.productName(),
                    line.productCode(),
                    line.quantity(),
                    line.unitSalePrice().amount(),
                    line.unitEstimatedCost(),
                    line.subtotal().amount());
        }

        Payment payment = sale.payment();
        jdbcTemplate.update(
                """
                INSERT INTO payment (
                    id, sale_id, method, amount, cash_payable, rounding_adjustment,
                    cash_received, change_amount, occurred_at, cash_rounding_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """,
                payment.id(),
                sale.id(),
                payment.method().name(),
                payment.amount().amount(),
                payment.cashPayable() == null ? null : payment.cashPayable().amount(),
                payment.roundingAdjustment(),
                payment.cashReceived() == null ? null : payment.cashReceived().amount(),
                payment.change() == null ? null : payment.change().amount(),
                Timestamp.from(payment.occurredAt()));
    }

    @Override
    public void saveCancellation(SaleCancellation cancellation) {
        jdbcTemplate.update(
                """
                INSERT INTO sale_cancellation (
                    id, sale_id, refund_method, amount, cash_payable,
                    rounding_adjustment, reason, actor_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                cancellation.id(),
                cancellation.saleId(),
                cancellation.refundMethod().name(),
                cancellation.amount().amount(),
                cancellation.cashPayable() == null
                        ? null
                        : cancellation.cashPayable().amount(),
                cancellation.roundingAdjustment(),
                cancellation.reason(),
                cancellation.actorId(),
                Timestamp.from(cancellation.occurredAt()));
    }

    @Override
    public boolean markVoided(UUID saleId) {
        return jdbcTemplate.update(
                        "UPDATE sale SET status = 'VOIDED' WHERE id = ? AND status = 'CONFIRMED'",
                        saleId)
                == 1;
    }

    @Override
    public SalePage<Sale> findRecent(int page, int size) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sale", Long.class);
        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT id
                FROM sale
                ORDER BY confirmed_at DESC, id DESC
                LIMIT ? OFFSET ?
                """,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                size,
                (long) page * size);
        List<Sale> sales = ids.stream().map(id -> findById(id).orElseThrow()).toList();
        return new SalePage<>(sales, page, size, total == null ? 0 : total);
    }

    private Optional<Sale> findOne(String sql, Object parameter) {
        return jdbcTemplate.query(sql, this::mapSaleHeader, parameter).stream()
                .findFirst()
                .map(this::complete);
    }

    private Sale complete(Sale header) {
        List<SaleLine> lines = jdbcTemplate.query(
                """
                SELECT id, product_id, product_name, product_code, quantity,
                       unit_sale_price, unit_estimated_cost, subtotal
                FROM sale_line
                WHERE sale_id = ?
                ORDER BY line_number
                """,
                this::mapLine,
                header.id());
        Payment payment = jdbcTemplate.queryForObject(
                """
                SELECT id, method, amount, cash_payable, rounding_adjustment,
                       cash_received, change_amount, occurred_at
                FROM payment
                WHERE sale_id = ?
                """,
                this::mapPayment,
                header.id());
        SaleCancellation cancellation = jdbcTemplate.query(
                        """
                        SELECT cancellation.id, cancellation.sale_id,
                               cancellation.refund_method, cancellation.amount,
                               cancellation.cash_payable, cancellation.rounding_adjustment,
                               cancellation.reason, cancellation.actor_id,
                               actor.display_name AS actor_display_name,
                               cancellation.occurred_at
                        FROM sale_cancellation cancellation
                        JOIN app_user actor ON actor.id = cancellation.actor_id
                        WHERE cancellation.sale_id = ?
                        """,
                        this::mapCancellation,
                        header.id())
                .stream()
                .findFirst()
                .orElse(null);
        return new Sale(
                header.id(),
                header.folio(),
                header.cashSessionId(),
                header.actorId(),
                header.actorDisplayName(),
                header.confirmedAt(),
                header.status(),
                header.total(),
                header.idempotencyKey(),
                header.requestHash(),
                lines,
                payment,
                cancellation);
    }

    private Sale mapSaleHeader(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Sale(
                resultSet.getObject("id", UUID.class),
                resultSet.getLong("folio"),
                resultSet.getObject("cash_session_id", UUID.class),
                resultSet.getObject("actor_id", UUID.class),
                resultSet.getString("actor_display_name"),
                resultSet.getTimestamp("confirmed_at").toInstant(),
                SaleStatus.valueOf(resultSet.getString("status")),
                Money.of(resultSet.getBigDecimal("total")),
                resultSet.getObject("idempotency_key", UUID.class),
                resultSet.getString("request_hash"),
                List.of(),
                null,
                null);
    }

    private SaleLine mapLine(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SaleLine(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("product_id", UUID.class),
                resultSet.getString("product_name"),
                resultSet.getString("product_code"),
                resultSet.getInt("quantity"),
                Money.of(resultSet.getBigDecimal("unit_sale_price")),
                resultSet.getBigDecimal("unit_estimated_cost"),
                Money.of(resultSet.getBigDecimal("subtotal")));
    }

    private Payment mapPayment(ResultSet resultSet, int rowNumber) throws SQLException {
        var received = resultSet.getBigDecimal("cash_received");
        var change = resultSet.getBigDecimal("change_amount");
        var cashPayable = resultSet.getBigDecimal("cash_payable");
        var adjustment = resultSet.getBigDecimal("rounding_adjustment");
        return new Payment(
                resultSet.getObject("id", UUID.class),
                PaymentMethod.valueOf(resultSet.getString("method")),
                Money.of(resultSet.getBigDecimal("amount")),
                cashPayable == null ? null : Money.of(cashPayable),
                adjustment,
                received == null ? null : Money.of(received),
                change == null ? null : Money.of(change),
                resultSet.getTimestamp("occurred_at").toInstant());
    }

    private SaleCancellation mapCancellation(ResultSet resultSet, int rowNumber)
            throws SQLException {
        var cashPayable = resultSet.getBigDecimal("cash_payable");
        return new SaleCancellation(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("sale_id", UUID.class),
                PaymentMethod.valueOf(resultSet.getString("refund_method")),
                Money.of(resultSet.getBigDecimal("amount")),
                cashPayable == null ? null : Money.of(cashPayable),
                resultSet.getBigDecimal("rounding_adjustment"),
                resultSet.getString("reason"),
                resultSet.getObject("actor_id", UUID.class),
                resultSet.getString("actor_display_name"),
                resultSet.getTimestamp("occurred_at").toInstant());
    }
}
