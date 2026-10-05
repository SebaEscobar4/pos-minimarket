package com.minimarket.pos.cash.application;

import com.minimarket.pos.cash.domain.CashInputRules;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementCategory;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.cash.domain.CashSessionStatus;
import com.minimarket.pos.cash.domain.ManualCashDirection;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashSessionService {

    private final CashSessionRepository repository;
    private final Clock clock;

    @Autowired
    public CashSessionService(CashSessionRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CashSessionService(CashSessionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Transactional
    public CashSession open(BigDecimal openingAmount, UUID actorId, String actorDisplayName) {
        var money = CashInputRules.openingAmount(openingAmount);
        if (actorId == null || actorDisplayName == null || actorDisplayName.isBlank()) {
            throw new ApplicationException(ProblemType.VALIDATION, "El usuario de apertura es obligatorio.");
        }
        if (repository.findOpen().isPresent()) {
            throw alreadyOpen();
        }
        CashSession session = new CashSession(
                UUID.randomUUID(),
                actorId,
                actorDisplayName,
                clock.instant(),
                money,
                CashSessionStatus.OPEN);
        if (!repository.create(session)) {
            throw alreadyOpen();
        }
        return session;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Transactional(readOnly = true)
    public Optional<CashSession> current() {
        return repository.findOpen();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CashMovement recordManualMovement(
            ManualCashDirection direction,
            CashMovementCategory category,
            BigDecimal amount,
            String reason,
            UUID actorId,
            String actorDisplayName) {
        if (direction == null || category == null || category.direction() != direction) {
            throw validation("La categoría no corresponde al tipo de movimiento.");
        }
        validateActor(actorId, actorDisplayName);
        Money movementAmount = CashInputRules.movementAmount(amount);
        String normalizedReason = CashInputRules.requiredReason(reason);
        CashSession session = repository.lockOpen().orElseThrow(CashSessionService::noOpenSession);
        CashTotals totals = repository.totals(session.id());
        BigDecimal currentExpected = expectedCash(session, totals).amount();
        BigDecimal resultingExpected = direction == ManualCashDirection.INCOME
                ? currentExpected.add(movementAmount.amount())
                : currentExpected.subtract(movementAmount.amount());
        if (resultingExpected.signum() < 0) {
            throw new ApplicationException(
                    ProblemType.CONFLICT,
                    "El retiro supera el efectivo esperado de la caja.");
        }
        if (resultingExpected.compareTo(Money.MAXIMUM_AMOUNT) > 0) {
            throw new ApplicationException(
                    ProblemType.CONFLICT,
                    "El movimiento supera el monto máximo permitido para la caja.");
        }
        CashMovement movement = new CashMovement(
                UUID.randomUUID(),
                session.id(),
                direction == ManualCashDirection.INCOME
                        ? CashMovementType.MANUAL_INCOME
                        : CashMovementType.MANUAL_WITHDRAWAL,
                category,
                movementAmount,
                normalizedReason,
                null,
                actorId,
                actorDisplayName,
                clock.instant());
        repository.append(movement);
        return movement;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Transactional
    public CashSummary summary(int page, int size) {
        CashInputRules.validatePage(page, size);
        CashSession session = repository.lockOpen().orElseThrow(CashSessionService::noOpenSession);
        CashTotals totals = repository.totals(session.id());
        return new CashSummary(
                session,
                totals,
                expectedCash(session, totals),
                repository.movements(session.id(), page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Transactional
    public CashCloseResult close(
            BigDecimal countedCash, UUID actorId, String actorDisplayName) {
        validateActor(actorId, actorDisplayName);
        Money counted = CashInputRules.countedCash(countedCash);
        CashSession session = repository.lockOpen().orElseThrow(CashSessionService::noOpenSession);
        CashTotals totals = repository.totals(session.id());
        Money expected = expectedCash(session, totals);
        BigDecimal difference = counted.amount().subtract(expected.amount());
        var closedAt = clock.instant();
        if (!repository.close(
                session.id(), actorId, closedAt, counted, expected, difference)) {
            throw noOpenSession();
        }
        return new CashCloseResult(
                session.id(),
                actorId,
                actorDisplayName,
                closedAt,
                session.openingAmount(),
                expected,
                counted,
                difference,
                totals);
    }

    private Money expectedCash(CashSession session, CashTotals totals) {
        BigDecimal expected = session.openingAmount()
                .amount()
                .add(totals.cashSales().amount())
                .add(totals.manualIncome().amount())
                .subtract(totals.manualWithdrawals().amount())
                .subtract(totals.cashRefunds().amount());
        if (expected.signum() < 0 || expected.compareTo(Money.MAXIMUM_AMOUNT) > 0) {
            throw new ApplicationException(
                    ProblemType.CONFLICT,
                    "Los movimientos de la caja producen un efectivo esperado inválido.");
        }
        return Money.of(expected);
    }

    private void validateActor(UUID actorId, String actorDisplayName) {
        if (actorId == null || actorDisplayName == null || actorDisplayName.isBlank()) {
            throw validation("El usuario responsable es obligatorio.");
        }
    }

    private static ApplicationException alreadyOpen() {
        return new ApplicationException(ProblemType.CONFLICT, "Ya existe una sesión de caja abierta.");
    }

    private static ApplicationException noOpenSession() {
        return new ApplicationException(ProblemType.CONFLICT, "No existe una sesión de caja abierta.");
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
