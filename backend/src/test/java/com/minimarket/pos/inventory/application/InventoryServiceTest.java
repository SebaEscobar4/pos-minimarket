package com.minimarket.pos.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minimarket.pos.inventory.application.InventoryRepository.BalanceView;
import com.minimarket.pos.inventory.application.InventoryRepository.LockedBalance;
import com.minimarket.pos.inventory.domain.AdjustmentDirection;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T09:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Mock
    private InventoryRepository repository;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsEntryAndBalanceTogetherWithAuditEvidence() {
        when(repository.lockBalance(PRODUCT_ID)).thenReturn(Optional.of(new LockedBalance(2, 4)));
        when(repository.updateBalance(PRODUCT_ID, 4, 7)).thenReturn(true);

        InventoryMovement movement = service.recordEntry(
                PRODUCT_ID, 5, " Recepción ", " FAC-1 ", ACTOR_ID, "Administración");

        assertThat(movement.type()).isEqualTo(InventoryMovementType.ENTRY);
        assertThat(movement.previousBalance()).isEqualTo(2);
        assertThat(movement.resultingBalance()).isEqualTo(7);
        assertThat(movement.reason()).isEqualTo("Recepción");
        assertThat(movement.reference()).isEqualTo("FAC-1");
        assertThat(movement.occurredAt()).isEqualTo(NOW);
        verify(repository).append(movement);
    }

    @Test
    void recordsPositiveAndNegativeAdjustments() {
        when(repository.lockBalance(PRODUCT_ID))
                .thenReturn(Optional.of(new LockedBalance(5, 1)), Optional.of(new LockedBalance(7, 2)));
        when(repository.updateBalance(PRODUCT_ID, 1, 7)).thenReturn(true);
        when(repository.updateBalance(PRODUCT_ID, 2, 4)).thenReturn(true);

        InventoryMovement positive = service.recordAdjustment(
                PRODUCT_ID,
                AdjustmentDirection.POSITIVE,
                2,
                "Conteo",
                ACTOR_ID,
                "Administración");
        InventoryMovement negative = service.recordAdjustment(
                PRODUCT_ID,
                AdjustmentDirection.NEGATIVE,
                3,
                "Merma",
                ACTOR_ID,
                "Administración");

        assertThat(positive.type()).isEqualTo(InventoryMovementType.ADJUSTMENT_IN);
        assertThat(positive.delta()).isEqualTo(2);
        assertThat(negative.type()).isEqualTo(InventoryMovementType.ADJUSTMENT_OUT);
        assertThat(negative.delta()).isEqualTo(-3);
    }

    @Test
    void rejectsNegativeOrOversizedBalancesWithoutAppendingMovement() {
        when(repository.lockBalance(PRODUCT_ID))
                .thenReturn(
                        Optional.of(new LockedBalance(1, 0)),
                        Optional.of(new LockedBalance(100_000_000, 0)));

        assertProblem(
                () -> service.recordAdjustment(
                        PRODUCT_ID,
                        AdjustmentDirection.NEGATIVE,
                        2,
                        "Conteo",
                        ACTOR_ID,
                        "Administración"),
                ProblemType.CONFLICT);
        assertProblem(
                () -> service.recordEntry(
                        PRODUCT_ID, 1, "Entrada", null, ACTOR_ID, "Administración"),
                ProblemType.CONFLICT);
    }

    @Test
    void rejectsMissingResourcesInvalidDirectionAndConcurrentVersionChanges() {
        when(repository.lockBalance(PRODUCT_ID))
                .thenReturn(Optional.empty(), Optional.of(new LockedBalance(2, 7)));
        when(repository.updateBalance(PRODUCT_ID, 7, 3)).thenReturn(false);

        assertProblem(
                () -> service.recordEntry(
                        PRODUCT_ID, 1, "Entrada", null, ACTOR_ID, "Administración"),
                ProblemType.NOT_FOUND);
        assertProblem(
                () -> service.recordEntry(null, 1, "Entrada", null, ACTOR_ID, "Administración"),
                ProblemType.VALIDATION);
        assertProblem(
                () -> service.recordAdjustment(
                        PRODUCT_ID, null, 1, "Conteo", ACTOR_ID, "Administración"),
                ProblemType.VALIDATION);
        assertProblem(
                () -> service.recordEntry(
                        PRODUCT_ID, 1, "Entrada", null, ACTOR_ID, "Administración"),
                ProblemType.CONFLICT);
    }

    @Test
    void returnsReconciliablePaginatedSnapshot() {
        InventoryMovement movement = movement();
        InventoryPage<InventoryMovement> page = new InventoryPage<>(List.of(movement), 0, 20, 1);
        when(repository.findBalance(PRODUCT_ID))
                .thenReturn(Optional.of(new BalanceView(PRODUCT_ID, "00123", "Café", 5, 3)));
        when(repository.findMovements(PRODUCT_ID, 0, 20)).thenReturn(page);

        InventorySnapshot snapshot = service.snapshot(PRODUCT_ID, 0, 20);

        assertThat(snapshot.currentBalance()).isEqualTo(5);
        assertThat(snapshot.movements().items()).containsExactly(movement);
        assertThat(snapshot.movements().totalPages()).isEqualTo(1);

        UUID missing = UUID.randomUUID();
        when(repository.findBalance(missing)).thenReturn(Optional.empty());
        assertProblem(() -> service.snapshot(missing, 0, 20), ProblemType.NOT_FOUND);
    }

    private InventoryMovement movement() {
        return new InventoryMovement(
                UUID.randomUUID(),
                PRODUCT_ID,
                InventoryMovementType.ENTRY,
                5,
                5,
                0,
                5,
                "Entrada",
                null,
                ACTOR_ID,
                "Administración",
                NOW);
    }

    private void assertProblem(Runnable action, ProblemType type) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(type);
    }
}
