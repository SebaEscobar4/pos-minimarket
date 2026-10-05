package com.minimarket.pos.inventory.application;

import com.minimarket.pos.inventory.application.InventoryRepository.BalanceView;
import com.minimarket.pos.inventory.application.InventoryRepository.LockedBalance;
import com.minimarket.pos.inventory.domain.AdjustmentDirection;
import com.minimarket.pos.inventory.domain.InventoryInputRules;
import com.minimarket.pos.inventory.domain.InventoryInputRules.Evidence;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository repository;
    private final Clock clock;

    @Autowired
    public InventoryService(InventoryRepository repository) {
        this(repository, Clock.systemUTC());
    }

    InventoryService(InventoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public InventoryMovement recordEntry(
            UUID productId,
            Integer quantity,
            String reason,
            String reference,
            UUID actorId,
            String actorDisplayName) {
        Evidence evidence = InventoryInputRules.entryEvidence(reason, reference);
        return apply(
                productId,
                InventoryMovementType.ENTRY,
                InventoryInputRules.quantity(quantity),
                evidence.reason(),
                evidence.reference(),
                actorId,
                actorDisplayName);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public InventoryMovement recordAdjustment(
            UUID productId,
            AdjustmentDirection direction,
            Integer quantity,
            String reason,
            UUID actorId,
            String actorDisplayName) {
        if (direction == null) {
            throw validation("La dirección del ajuste es obligatoria.");
        }
        InventoryMovementType type = direction == AdjustmentDirection.POSITIVE
                ? InventoryMovementType.ADJUSTMENT_IN
                : InventoryMovementType.ADJUSTMENT_OUT;
        return apply(
                productId,
                type,
                InventoryInputRules.quantity(quantity),
                InventoryInputRules.requiredReason(reason),
                null,
                actorId,
                actorDisplayName);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public InventorySnapshot snapshot(UUID productId, int page, int size) {
        InventoryInputRules.validatePage(page, size);
        BalanceView balance = repository.findBalance(productId).orElseThrow(InventoryService::notFound);
        return new InventorySnapshot(
                balance.productId(),
                balance.productCode(),
                balance.productName(),
                balance.quantity(),
                balance.version(),
                repository.findMovements(productId, page, size));
    }

    private InventoryMovement apply(
            UUID productId,
            InventoryMovementType type,
            int quantity,
            String reason,
            String reference,
            UUID actorId,
            String actorDisplayName) {
        if (productId == null || actorId == null) {
            throw validation("El producto y el actor son obligatorios.");
        }
        LockedBalance balance = repository.lockBalance(productId).orElseThrow(InventoryService::notFound);
        int delta = type.deltaFor(quantity);
        long resulting = (long) balance.quantity() + delta;
        if (resulting < 0) {
            throw new ApplicationException(
                    ProblemType.CONFLICT, "El movimiento dejaría el saldo de inventario negativo.");
        }
        if (resulting > InventoryInputRules.MAXIMUM_BALANCE) {
            throw new ApplicationException(
                    ProblemType.CONFLICT, "El movimiento supera el saldo máximo permitido.");
        }
        InventoryMovement movement = new InventoryMovement(
                UUID.randomUUID(),
                productId,
                type,
                quantity,
                delta,
                balance.quantity(),
                (int) resulting,
                reason,
                reference,
                actorId,
                actorDisplayName,
                clock.instant());
        if (!repository.updateBalance(productId, balance.version(), movement.resultingBalance())) {
            throw new ApplicationException(
                    ProblemType.CONFLICT, "El saldo cambió durante la operación; inténtalo nuevamente.");
        }
        repository.append(movement);
        return movement;
    }

    private static ApplicationException notFound() {
        return new ApplicationException(ProblemType.NOT_FOUND, "El saldo de inventario solicitado no existe.");
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }
}
