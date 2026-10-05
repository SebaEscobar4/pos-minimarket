package com.minimarket.pos.cash.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.cash.domain.CashSessionStatus;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementCategory;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.cash.domain.ManualCashDirection;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Mock
    private CashSessionRepository repository;

    private CashSessionService service;

    @BeforeEach
    void setUp() {
        service = new CashSessionService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void opensAnAuditableSessionWithExactClp() {
        when(repository.findOpen()).thenReturn(Optional.empty());
        when(repository.create(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        CashSession session = service.open(new BigDecimal("25000"), ACTOR_ID, "Vendedor");

        assertThat(session.openedBy()).isEqualTo(ACTOR_ID);
        assertThat(session.openedByDisplayName()).isEqualTo("Vendedor");
        assertThat(session.openedAt()).isEqualTo(NOW);
        assertThat(session.openingAmount().amount()).isEqualByComparingTo("25000");
        assertThat(session.status()).isEqualTo(CashSessionStatus.OPEN);
    }

    @Test
    void rejectsAnExistingSessionAndAConcurrentUniqueConflict() {
        CashSession existing = existingSession();
        when(repository.findOpen()).thenReturn(Optional.of(existing), Optional.empty());
        when(repository.create(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertConflict(() -> service.open(BigDecimal.ZERO, ACTOR_ID, "Vendedor"));
        assertConflict(() -> service.open(BigDecimal.ZERO, ACTOR_ID, "Vendedor"));
    }

    @Test
    void validatesAmountAndActorBeforeWriting() {
        assertProblem(
                () -> service.open(new BigDecimal("1.5"), ACTOR_ID, "Vendedor"),
                ProblemType.VALIDATION);
        assertProblem(
                () -> service.open(BigDecimal.ZERO, null, "Vendedor"), ProblemType.VALIDATION);
        assertProblem(
                () -> service.open(BigDecimal.ZERO, ACTOR_ID, " "), ProblemType.VALIDATION);

        verifyNoInteractions(repository);
    }

    @Test
    void returnsTheCurrentSessionWhenPresent() {
        CashSession existing = existingSession();
        when(repository.findOpen()).thenReturn(Optional.of(existing));

        assertThat(service.current()).contains(existing);
    }

    @Test
    void recordsApprovedManualIncomeAndWithdrawalCategories() {
        CashSession existing = existingSession();
        when(repository.lockOpen()).thenReturn(Optional.of(existing), Optional.of(existing));
        when(repository.totals(existing.id())).thenReturn(totals(0, 0), totals(5000, 0));

        CashMovement income = service.recordManualMovement(
                ManualCashDirection.INCOME,
                CashMovementCategory.CASH_REPLENISHMENT,
                new BigDecimal("5000"),
                " Refuerzo para cambio ",
                ACTOR_ID,
                "Administración");
        CashMovement withdrawal = service.recordManualMovement(
                ManualCashDirection.WITHDRAWAL,
                CashMovementCategory.OPERATING_EXPENSE,
                new BigDecimal("3000"),
                "Compra de bolsas",
                ACTOR_ID,
                "Administración");

        assertThat(income.type()).isEqualTo(CashMovementType.MANUAL_INCOME);
        assertThat(income.reason()).isEqualTo("Refuerzo para cambio");
        assertThat(withdrawal.type()).isEqualTo(CashMovementType.MANUAL_WITHDRAWAL);
        assertThat(withdrawal.amount().amount()).isEqualByComparingTo("3000");
        verify(repository).append(income);
        verify(repository).append(withdrawal);
    }

    @Test
    void rejectsWrongCategoryNoOpenSessionAndInvalidExpectedCash() {
        assertProblem(
                () -> service.recordManualMovement(
                        ManualCashDirection.INCOME,
                        CashMovementCategory.SUPPLIER_PAYMENT,
                        BigDecimal.ONE,
                        "Motivo",
                        ACTOR_ID,
                        "Administración"),
                ProblemType.VALIDATION);

        when(repository.lockOpen()).thenReturn(Optional.empty(), Optional.of(existingSession()));
        assertProblem(
                () -> service.recordManualMovement(
                        ManualCashDirection.INCOME,
                        CashMovementCategory.OTHER_INCOME,
                        BigDecimal.ONE,
                        "Motivo",
                        ACTOR_ID,
                        "Administración"),
                ProblemType.CONFLICT);
        when(repository.totals(org.mockito.ArgumentMatchers.any())).thenReturn(totals(0, 0));
        assertProblem(
                () -> service.recordManualMovement(
                        ManualCashDirection.WITHDRAWAL,
                        CashMovementCategory.OTHER_WITHDRAWAL,
                        new BigDecimal("25001"),
                        "Motivo",
                        ACTOR_ID,
                        "Administración"),
                ProblemType.CONFLICT);
    }

    @Test
    void returnsAReconciliableSummaryAndClosesWithDifference() {
        CashSession existing = existingSession();
        CashTotals totals = totals(5000, 3000);
        CashPage<CashMovement> page = new CashPage<>(List.of(), 0, 20, 0);
        when(repository.lockOpen()).thenReturn(Optional.of(existing), Optional.of(existing));
        when(repository.totals(existing.id())).thenReturn(totals);
        when(repository.movements(existing.id(), 0, 20)).thenReturn(page);
        when(repository.close(
                        org.mockito.ArgumentMatchers.eq(existing.id()),
                        org.mockito.ArgumentMatchers.eq(ACTOR_ID),
                        org.mockito.ArgumentMatchers.eq(NOW),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        CashSummary summary = service.summary(0, 20);
        CashCloseResult result =
                service.close(new BigDecimal("28000"), ACTOR_ID, "Administración");

        assertThat(summary.expectedCash().amount()).isEqualByComparingTo("27000");
        assertThat(summary.movements().totalPages()).isZero();
        assertThat(result.expectedCash().amount()).isEqualByComparingTo("27000");
        assertThat(result.countedCash().amount()).isEqualByComparingTo("28000");
        assertThat(result.difference()).isEqualByComparingTo("1000");
    }

    @Test
    void rejectsSummaryAndCloseWithoutAnOpenSession() {
        when(repository.lockOpen()).thenReturn(Optional.empty(), Optional.empty());

        assertProblem(() -> service.summary(0, 20), ProblemType.CONFLICT);
        assertProblem(
                () -> service.close(BigDecimal.ZERO, ACTOR_ID, "Vendedor"),
                ProblemType.CONFLICT);
    }

    private CashSession existingSession() {
        return new CashSession(
                UUID.randomUUID(),
                ACTOR_ID,
                "Vendedor",
                NOW,
                Money.of(new BigDecimal("25000")),
                CashSessionStatus.OPEN);
    }

    private CashTotals totals(long income, long withdrawals) {
        return new CashTotals(
                Money.of(BigDecimal.ZERO),
                Money.of(BigDecimal.valueOf(income)),
                Money.of(BigDecimal.valueOf(withdrawals)),
                Money.of(BigDecimal.ZERO),
                Money.of(BigDecimal.ZERO),
                Money.of(BigDecimal.ZERO));
    }

    private void assertConflict(Runnable action) {
        assertProblem(action, ProblemType.CONFLICT);
    }

    private void assertProblem(Runnable action, ProblemType problemType) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(problemType);
    }
}
