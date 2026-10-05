package com.minimarket.pos.sales.application;

import com.minimarket.pos.cash.application.CashSessionRepository;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.catalog.application.ProductRepository;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import com.minimarket.pos.inventory.application.InventoryRepository;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.sales.application.SaleRequestHasher.NormalizedLine;
import com.minimarket.pos.sales.domain.Payment;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleInputRules;
import com.minimarket.pos.sales.domain.SaleLine;
import com.minimarket.pos.sales.domain.SaleStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CashSessionRepository cashRepository;
    private final Clock clock;

    @Autowired
    public SaleService(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CashSessionRepository cashRepository,
            Clock clock) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.cashRepository = cashRepository;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @Transactional
    public SaleConfirmation confirm(SaleCommand command, UUID actorId, String actorDisplayName) {
        validateActor(actorId, actorDisplayName);
        if (command == null) {
            throw validation("Los datos de la venta son obligatorios.");
        }
        SaleInputRules.validateIntent(
                command.idempotencyKey(), command.lines(), command.paymentMethod());
        List<NormalizedLine> normalizedLines = normalizeLines(command.lines());
        BigDecimal normalizedReceived = normalizeCashReceived(
                command.paymentMethod(), command.cashReceived());
        String requestHash = SaleRequestHasher.hash(
                normalizedLines, command.paymentMethod(), normalizedReceived);

        saleRepository.lockIdempotencyKey(command.idempotencyKey());
        Optional<Sale> previous = saleRepository.findByIdempotencyKey(command.idempotencyKey());
        if (previous.isPresent()) {
            if (!previous.orElseThrow().requestHash().equals(requestHash)) {
                throw new ApplicationException(
                        ProblemType.CONFLICT,
                        "La clave de confirmación ya fue utilizada con una venta diferente.");
            }
            return new SaleConfirmation(previous.orElseThrow(), false);
        }

        var cashSession = cashRepository.lockOpen().orElseThrow(SaleService::noOpenCashSession);
        Instant occurredAt = clock.instant();
        List<PreparedLine> prepared = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (NormalizedLine requested : normalizedLines) {
            Product product = productRepository
                    .findById(requested.productId())
                    .orElseThrow(SaleService::productNotFound);
            if (product.status() != ProductStatus.ACTIVE) {
                throw new ApplicationException(
                        ProblemType.CONFLICT, "Uno de los productos ya no está activo para venta.");
            }
            Money unitPrice = salePrice(product);
            var balance = inventoryRepository
                    .lockBalance(product.id())
                    .orElseThrow(SaleService::productNotFound);
            if (balance.quantity() < requested.quantity()) {
                throw new ApplicationException(
                        ProblemType.CONFLICT,
                        "No existe stock suficiente para " + product.name() + ".");
            }
            Money subtotal = exactMoney(unitPrice.amount().multiply(BigDecimal.valueOf(requested.quantity())));
            totalAmount = totalAmount.add(subtotal.amount());
            prepared.add(new PreparedLine(product, requested.quantity(), balance, subtotal));
        }

        Money total = exactMoney(totalAmount);
        Payment payment = SaleInputRules.payment(
                command.paymentMethod(), normalizedReceived, total, occurredAt);
        Sale sale = createSale(
                cashSession.id(),
                actorId,
                actorDisplayName.strip(),
                command.idempotencyKey(),
                requestHash,
                prepared,
                payment,
                total,
                occurredAt);

        saleRepository.save(sale);
        applyInventory(prepared, sale, actorId, actorDisplayName.strip(), occurredAt);
        if (payment.method() == PaymentMethod.CASH
                && payment.cashPayable().amount().signum() > 0) {
            cashRepository.append(new CashMovement(
                    UUID.randomUUID(),
                    cashSession.id(),
                    CashMovementType.CASH_SALE,
                    null,
                    payment.cashPayable(),
                    null,
                    sale.displayFolio(),
                    actorId,
                    actorDisplayName.strip(),
                    occurredAt));
        }
        return new SaleConfirmation(sale, true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public SalePage<Sale> recent(int page, int size) {
        SaleInputRules.validatePage(page, size);
        return saleRepository.findRecent(page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Sale detail(UUID id) {
        if (id == null) {
            throw validation("El identificador de venta es obligatorio.");
        }
        return saleRepository.findById(id).orElseThrow(SaleService::saleNotFound);
    }

    private Sale createSale(
            UUID cashSessionId,
            UUID actorId,
            String actorDisplayName,
            UUID idempotencyKey,
            String requestHash,
            List<PreparedLine> prepared,
            Payment payment,
            Money total,
            Instant occurredAt) {
        List<SaleLine> lines = new ArrayList<>();
        for (PreparedLine item : prepared) {
            Product product = item.product();
            lines.add(new SaleLine(
                    UUID.randomUUID(),
                    product.id(),
                    product.name(),
                    product.code(),
                    item.quantity(),
                    salePrice(product),
                    product.purchasePrice(),
                    item.subtotal()));
        }
        return new Sale(
                UUID.randomUUID(),
                saleRepository.nextFolio(),
                cashSessionId,
                actorId,
                actorDisplayName,
                occurredAt,
                SaleStatus.CONFIRMED,
                total,
                idempotencyKey,
                requestHash,
                List.copyOf(lines),
                payment,
                null);
    }

    private void applyInventory(
            List<PreparedLine> prepared,
            Sale sale,
            UUID actorId,
            String actorDisplayName,
            Instant occurredAt) {
        for (PreparedLine item : prepared) {
            int resulting = item.balance().quantity() - item.quantity();
            if (!inventoryRepository.updateBalance(
                    item.product().id(), item.balance().version(), resulting)) {
                throw new ApplicationException(
                        ProblemType.CONFLICT,
                        "El inventario cambió durante la confirmación. Intenta nuevamente.");
            }
            inventoryRepository.append(new InventoryMovement(
                    UUID.randomUUID(),
                    item.product().id(),
                    InventoryMovementType.SALE_OUT,
                    item.quantity(),
                    -item.quantity(),
                    item.balance().quantity(),
                    resulting,
                    null,
                    sale.displayFolio(),
                    actorId,
                    actorDisplayName,
                    occurredAt));
        }
    }

    private List<NormalizedLine> normalizeLines(List<SaleCommand.Line> requestedLines) {
        Map<UUID, Integer> quantities = new TreeMap<>();
        for (SaleCommand.Line line : requestedLines) {
            if (line == null || line.productId() == null) {
                throw validation("Cada línea debe indicar un producto.");
            }
            int quantity = SaleInputRules.quantity(line.quantity());
            try {
                quantities.merge(line.productId(), quantity, Math::addExact);
            } catch (ArithmeticException exception) {
                throw validation("La cantidad acumulada de un producto es demasiado alta.");
            }
        }
        if (quantities.size() > SaleInputRules.MAXIMUM_LINES) {
            throw validation("La venta admite hasta 100 productos distintos.");
        }
        return quantities.entrySet().stream()
                .map(entry -> new NormalizedLine(entry.getKey(),
                        SaleInputRules.quantity(entry.getValue())))
                .toList();
    }

    private BigDecimal normalizeCashReceived(PaymentMethod method, BigDecimal value) {
        if (method == PaymentMethod.CASH) {
            try {
                return Money.of(value).amount();
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw validation("El efectivo recibido debe ser un monto CLP entero válido.");
            }
        }
        if (value != null) {
            throw validation("El efectivo recibido solo corresponde a pagos en efectivo.");
        }
        return null;
    }

    private Money salePrice(Product product) {
        try {
            return Money.of(product.salePrice());
        } catch (IllegalArgumentException exception) {
            throw validation(
                    "El precio de venta de " + product.name()
                            + " debe ser un monto CLP entero antes de venderlo.");
        }
    }

    private Money exactMoney(BigDecimal amount) {
        try {
            return Money.of(amount);
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(
                    ProblemType.CONFLICT, "El total de la venta supera el monto permitido.");
        }
    }

    private void validateActor(UUID actorId, String displayName) {
        if (actorId == null || displayName == null || displayName.isBlank()) {
            throw validation("El usuario responsable es obligatorio.");
        }
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }

    private static ApplicationException noOpenCashSession() {
        return new ApplicationException(
                ProblemType.CONFLICT, "Debes abrir la caja antes de registrar una venta.");
    }

    private static ApplicationException productNotFound() {
        return new ApplicationException(
                ProblemType.NOT_FOUND, "Uno de los productos de la venta no existe.");
    }

    private static ApplicationException saleNotFound() {
        return new ApplicationException(ProblemType.NOT_FOUND, "La venta solicitada no existe.");
    }

    private record PreparedLine(
            Product product,
            int quantity,
            InventoryRepository.LockedBalance balance,
            Money subtotal) {}
}
