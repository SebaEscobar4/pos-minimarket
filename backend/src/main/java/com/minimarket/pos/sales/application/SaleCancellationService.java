package com.minimarket.pos.sales.application;

import com.minimarket.pos.cash.application.CashSessionRepository;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.inventory.application.InventoryRepository;
import com.minimarket.pos.inventory.domain.InventoryInputRules;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleCancellation;
import com.minimarket.pos.sales.domain.SaleCancellationRules;
import com.minimarket.pos.sales.domain.SaleLine;
import com.minimarket.pos.sales.domain.SaleStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleCancellationService {

    private final SaleRepository saleRepository;
    private final InventoryRepository inventoryRepository;
    private final CashSessionRepository cashRepository;
    private final Clock clock;

    public SaleCancellationService(
            SaleRepository saleRepository,
            InventoryRepository inventoryRepository,
            CashSessionRepository cashRepository,
            Clock clock) {
        this.saleRepository = saleRepository;
        this.inventoryRepository = inventoryRepository;
        this.cashRepository = cashRepository;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Sale cancel(
            UUID saleId, CancelSaleCommand command, UUID actorId, String actorDisplayName) {
        if (command == null) {
            throw validation("Los datos de la anulación son obligatorios.");
        }
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(SaleCancellationService::saleNotFound);
        if (sale.status() != SaleStatus.CONFIRMED || sale.cancellation() != null) {
            throw new ApplicationException(ProblemType.CONFLICT, "La venta ya fue anulada.");
        }

        Instant occurredAt = clock.instant();
        SaleCancellation cancellation = SaleCancellationRules.create(
                sale.id(),
                command.refundMethod(),
                sale.total(),
                command.reason(),
                actorId,
                actorDisplayName,
                occurredAt);

        var cashSession = cancellation.refundMethod() == PaymentMethod.CASH
                ? cashRepository
                        .lockOpen()
                        .orElseThrow(SaleCancellationService::noOpenCashSession)
                : null;
        List<PreparedReversal> reversals = lockInventory(sale);

        saleRepository.saveCancellation(cancellation);
        applyInventory(reversals, sale, cancellation, occurredAt);
        if (cashSession != null && cancellation.cashPayable().amount().signum() > 0) {
            cashRepository.append(new CashMovement(
                    UUID.randomUUID(),
                    cashSession.id(),
                    CashMovementType.CASH_REFUND,
                    null,
                    cancellation.cashPayable(),
                    null,
                    sale.displayFolio(),
                    actorId,
                    actorDisplayName.strip(),
                    occurredAt));
        }
        if (!saleRepository.markVoided(sale.id())) {
            throw new ApplicationException(
                    ProblemType.CONFLICT, "La venta cambió durante la anulación.");
        }
        return saleRepository.findById(sale.id()).orElseThrow();
    }

    private List<PreparedReversal> lockInventory(Sale sale) {
        List<SaleLine> ordered = sale.lines().stream()
                .sorted(Comparator.comparing(SaleLine::productId))
                .toList();
        List<PreparedReversal> prepared = new ArrayList<>();
        for (SaleLine line : ordered) {
            var balance = inventoryRepository
                    .lockBalance(line.productId())
                    .orElseThrow(SaleCancellationService::productNotFound);
            long resulting = (long) balance.quantity() + line.quantity();
            if (resulting > InventoryInputRules.MAXIMUM_BALANCE) {
                throw new ApplicationException(
                        ProblemType.CONFLICT,
                        "La reposición supera el saldo máximo permitido para "
                                + line.productName()
                                + ".");
            }
            prepared.add(new PreparedReversal(line, balance, (int) resulting));
        }
        return prepared;
    }

    private void applyInventory(
            List<PreparedReversal> reversals,
            Sale sale,
            SaleCancellation cancellation,
            Instant occurredAt) {
        for (PreparedReversal prepared : reversals) {
            SaleLine line = prepared.line();
            if (!inventoryRepository.updateBalance(
                    line.productId(),
                    prepared.balance().version(),
                    prepared.resultingBalance())) {
                throw new ApplicationException(
                        ProblemType.CONFLICT,
                        "El inventario cambió durante la anulación. Intenta nuevamente.");
            }
            inventoryRepository.append(new InventoryMovement(
                    UUID.randomUUID(),
                    line.productId(),
                    InventoryMovementType.SALE_REVERSAL,
                    line.quantity(),
                    line.quantity(),
                    prepared.balance().quantity(),
                    prepared.resultingBalance(),
                    cancellation.reason(),
                    sale.displayFolio(),
                    cancellation.actorId(),
                    cancellation.actorDisplayName(),
                    occurredAt));
        }
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }

    private static ApplicationException saleNotFound() {
        return new ApplicationException(ProblemType.NOT_FOUND, "La venta solicitada no existe.");
    }

    private static ApplicationException productNotFound() {
        return new ApplicationException(
                ProblemType.CONFLICT, "No existe el saldo de uno de los productos vendidos.");
    }

    private static ApplicationException noOpenCashSession() {
        return new ApplicationException(
                ProblemType.CONFLICT,
                "Debes abrir la caja antes de registrar una devolución en efectivo.");
    }

    private record PreparedReversal(
            SaleLine line,
            InventoryRepository.LockedBalance balance,
            int resultingBalance) {}
}
