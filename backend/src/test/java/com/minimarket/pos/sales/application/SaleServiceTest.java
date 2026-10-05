package com.minimarket.pos.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minimarket.pos.cash.application.CashSessionRepository;
import com.minimarket.pos.cash.domain.CashMovement;
import com.minimarket.pos.cash.domain.CashMovementType;
import com.minimarket.pos.cash.domain.CashSession;
import com.minimarket.pos.cash.domain.CashSessionStatus;
import com.minimarket.pos.catalog.application.ProductRepository;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import com.minimarket.pos.inventory.application.InventoryRepository;
import com.minimarket.pos.inventory.application.InventoryRepository.LockedBalance;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.sales.application.SaleRequestHasher.NormalizedLine;
import com.minimarket.pos.sales.domain.Payment;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleLine;
import com.minimarket.pos.sales.domain.SaleStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import com.minimarket.pos.shared.domain.Money;
import java.math.BigDecimal;
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
class SaleServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T15:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID CASH_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000602");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000603");

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CashSessionRepository cashRepository;

    private SaleService service;

    @BeforeEach
    void setUp() {
        service = new SaleService(
                saleRepository,
                productRepository,
                inventoryRepository,
                cashRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void confirmsCashSaleWithHistoricalLineStockAndCashEffects() {
        UUID key = UUID.randomUUID();
        when(saleRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(cashRepository.lockOpen()).thenReturn(Optional.of(cashSession()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(inventoryRepository.lockBalance(PRODUCT_ID))
                .thenReturn(Optional.of(new LockedBalance(5, 4)));
        when(inventoryRepository.updateBalance(PRODUCT_ID, 4, 3)).thenReturn(true);
        when(saleRepository.nextFolio()).thenReturn(12L);

        SaleConfirmation confirmation = service.confirm(
                command(key, PaymentMethod.CASH, new BigDecimal("5000")),
                ACTOR_ID,
                " Administración ");

        assertThat(confirmation.created()).isTrue();
        assertThat(confirmation.sale().displayFolio()).isEqualTo("V-000012");
        assertThat(confirmation.sale().total().amount()).isEqualByComparingTo("3000");
        assertThat(confirmation.sale().payment().cashPayable().amount()).isEqualByComparingTo("3000");
        assertThat(confirmation.sale().payment().roundingAdjustment()).isEqualByComparingTo("0");
        assertThat(confirmation.sale().payment().change().amount()).isEqualByComparingTo("2000");
        assertThat(confirmation.sale().lines().getFirst().productName()).isEqualTo("Arroz");

        ArgumentCaptor<InventoryMovement> inventory = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryRepository).append(inventory.capture());
        assertThat(inventory.getValue().type()).isEqualTo(InventoryMovementType.SALE_OUT);
        assertThat(inventory.getValue().resultingBalance()).isEqualTo(3);
        assertThat(inventory.getValue().reference()).isEqualTo("V-000012");

        ArgumentCaptor<CashMovement> cash = ArgumentCaptor.forClass(CashMovement.class);
        verify(cashRepository).append(cash.capture());
        assertThat(cash.getValue().type()).isEqualTo(CashMovementType.CASH_SALE);
        assertThat(cash.getValue().amount().amount()).isEqualByComparingTo("3000");
        verify(saleRepository).save(confirmation.sale());
    }

    @Test
    void returnsThePreviousSaleForTheSameIdempotentIntent() {
        UUID key = UUID.randomUUID();
        String hash = SaleRequestHasher.hash(
                List.of(new NormalizedLine(PRODUCT_ID, 2)),
                PaymentMethod.CARD,
                null);
        Sale previous = previousSale(key, hash);
        when(saleRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(previous));

        SaleConfirmation confirmation = service.confirm(
                command(key, PaymentMethod.CARD, null), ACTOR_ID, "Administración");

        assertThat(confirmation.created()).isFalse();
        assertThat(confirmation.sale()).isSameAs(previous);
        verify(cashRepository, never()).lockOpen();
        verify(saleRepository, never()).save(any());
    }

    @Test
    void rejectsReusedKeyWithDifferentIntentAndInsufficientStock() {
        UUID key = UUID.randomUUID();
        when(saleRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(previousSale(key, "0".repeat(64))));
        assertProblem(
                () -> service.confirm(
                        command(key, PaymentMethod.CARD, null), ACTOR_ID, "Administración"),
                ProblemType.CONFLICT);

        UUID stockKey = UUID.randomUUID();
        when(saleRepository.findByIdempotencyKey(stockKey)).thenReturn(Optional.empty());
        when(cashRepository.lockOpen()).thenReturn(Optional.of(cashSession()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(inventoryRepository.lockBalance(PRODUCT_ID))
                .thenReturn(Optional.of(new LockedBalance(1, 1)));
        assertProblem(
                () -> service.confirm(
                        command(stockKey, PaymentMethod.CARD, null), ACTOR_ID, "Administración"),
                ProblemType.CONFLICT);
        verify(saleRepository, never()).save(any());
    }

    @Test
    void rejectsMissingCashSessionAndFractionalSalePrices() {
        UUID key = UUID.randomUUID();
        when(saleRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(cashRepository.lockOpen()).thenReturn(Optional.empty());
        assertProblem(
                () -> service.confirm(
                        command(key, PaymentMethod.TRANSFER, null), ACTOR_ID, "Administración"),
                ProblemType.CONFLICT);

        UUID fractionalKey = UUID.randomUUID();
        when(saleRepository.findByIdempotencyKey(fractionalKey)).thenReturn(Optional.empty());
        when(cashRepository.lockOpen()).thenReturn(Optional.of(cashSession()));
        Product fractional = new Product(
                PRODUCT_ID,
                "001",
                "Fraccionario",
                UUID.randomUUID(),
                "Otros",
                new BigDecimal("1000.00"),
                new BigDecimal("1500.50"),
                1,
                ProductStatus.ACTIVE,
                5,
                NOW,
                NOW);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(fractional));
        assertProblem(
                () -> service.confirm(
                        command(fractionalKey, PaymentMethod.TRANSFER, null),
                        ACTOR_ID,
                        "Administración"),
                ProblemType.VALIDATION);
    }

    private SaleCommand command(UUID key, PaymentMethod method, BigDecimal received) {
        return new SaleCommand(
                key, List.of(new SaleCommand.Line(PRODUCT_ID, 2)), method, received);
    }

    private Product product() {
        return new Product(
                PRODUCT_ID,
                "001",
                "Arroz",
                UUID.randomUUID(),
                "Abarrotes",
                new BigDecimal("1000.25"),
                new BigDecimal("1500.00"),
                1,
                ProductStatus.ACTIVE,
                5,
                NOW,
                NOW);
    }

    private CashSession cashSession() {
        return new CashSession(
                CASH_SESSION_ID,
                ACTOR_ID,
                "Administración",
                NOW,
                Money.of(new BigDecimal("10000")),
                CashSessionStatus.OPEN);
    }

    private Sale previousSale(UUID key, String hash) {
        Money total = Money.of(new BigDecimal("3000"));
        Payment payment = new Payment(
                UUID.randomUUID(), PaymentMethod.CARD, total, null, null, null, null, NOW);
        SaleLine line = new SaleLine(
                UUID.randomUUID(),
                PRODUCT_ID,
                "Arroz",
                "001",
                2,
                Money.of(new BigDecimal("1500")),
                new BigDecimal("1000.25"),
                total);
        return new Sale(
                UUID.randomUUID(),
                1,
                CASH_SESSION_ID,
                ACTOR_ID,
                "Administración",
                NOW,
                SaleStatus.CONFIRMED,
                total,
                key,
                hash,
                List.of(line),
                payment,
                null);
    }

    private void assertProblem(Runnable action, ProblemType type) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(type);
    }
}
