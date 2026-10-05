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
import com.minimarket.pos.inventory.application.InventoryRepository;
import com.minimarket.pos.inventory.application.InventoryRepository.LockedBalance;
import com.minimarket.pos.inventory.domain.InventoryMovement;
import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.sales.domain.Payment;
import com.minimarket.pos.sales.domain.PaymentMethod;
import com.minimarket.pos.sales.domain.Sale;
import com.minimarket.pos.sales.domain.SaleCancellation;
import com.minimarket.pos.sales.domain.SaleLine;
import com.minimarket.pos.sales.domain.SaleStatus;
import com.minimarket.pos.shared.application.error.ApplicationException;
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
class SaleCancellationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T20:00:00Z");
    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID CASH_ID = UUID.randomUUID();

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CashSessionRepository cashRepository;

    private SaleCancellationService service;

    @BeforeEach
    void setUp() {
        service = new SaleCancellationService(
                saleRepository,
                inventoryRepository,
                cashRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void cancelsCashSaleWithInventoryAndCurrentCashEffects() {
        Sale sale = sale(SaleStatus.CONFIRMED, null);
        when(saleRepository.findByIdForUpdate(SALE_ID)).thenReturn(Optional.of(sale));
        when(cashRepository.lockOpen()).thenReturn(Optional.of(cashSession()));
        when(inventoryRepository.lockBalance(PRODUCT_ID))
                .thenReturn(Optional.of(new LockedBalance(3, 2)));
        when(inventoryRepository.updateBalance(PRODUCT_ID, 2, 5)).thenReturn(true);
        when(saleRepository.markVoided(SALE_ID)).thenReturn(true);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));

        service.cancel(
                SALE_ID,
                new CancelSaleCommand(PaymentMethod.CASH, "Error de cobro"),
                ACTOR_ID,
                "Administración");

        ArgumentCaptor<SaleCancellation> cancellation =
                ArgumentCaptor.forClass(SaleCancellation.class);
        verify(saleRepository).saveCancellation(cancellation.capture());
        assertThat(cancellation.getValue().cashPayable().amount()).isEqualByComparingTo("410");

        ArgumentCaptor<InventoryMovement> inventory =
                ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryRepository).append(inventory.capture());
        assertThat(inventory.getValue().type()).isEqualTo(InventoryMovementType.SALE_REVERSAL);
        assertThat(inventory.getValue().resultingBalance()).isEqualTo(5);

        ArgumentCaptor<CashMovement> cash = ArgumentCaptor.forClass(CashMovement.class);
        verify(cashRepository).append(cash.capture());
        assertThat(cash.getValue().type()).isEqualTo(CashMovementType.CASH_REFUND);
        assertThat(cash.getValue().amount().amount()).isEqualByComparingTo("410");
        verify(saleRepository).markVoided(SALE_ID);
    }

    @Test
    void recordsElectronicRefundWithoutTouchingCash() {
        Sale sale = sale(SaleStatus.CONFIRMED, null);
        when(saleRepository.findByIdForUpdate(SALE_ID)).thenReturn(Optional.of(sale));
        when(inventoryRepository.lockBalance(PRODUCT_ID))
                .thenReturn(Optional.of(new LockedBalance(3, 2)));
        when(inventoryRepository.updateBalance(PRODUCT_ID, 2, 5)).thenReturn(true);
        when(saleRepository.markVoided(SALE_ID)).thenReturn(true);
        when(saleRepository.findById(SALE_ID)).thenReturn(Optional.of(sale));

        service.cancel(
                SALE_ID,
                new CancelSaleCommand(PaymentMethod.CARD, "Cobro duplicado"),
                ACTOR_ID,
                "Administración");

        verify(cashRepository, never()).lockOpen();
        verify(cashRepository, never()).append(any());
        verify(saleRepository).saveCancellation(any());
    }

    @Test
    void rejectsAlreadyVoidedSaleAndCashRefundWithoutOpenSession() {
        SaleCancellation previous = cancellation();
        when(saleRepository.findByIdForUpdate(SALE_ID))
                .thenReturn(Optional.of(sale(SaleStatus.VOIDED, previous)));
        assertThatThrownBy(() -> service.cancel(
                        SALE_ID,
                        new CancelSaleCommand(PaymentMethod.CARD, "Repetida"),
                        ACTOR_ID,
                        "Administración"))
                .isInstanceOf(ApplicationException.class);

        when(saleRepository.findByIdForUpdate(SALE_ID))
                .thenReturn(Optional.of(sale(SaleStatus.CONFIRMED, null)));
        when(cashRepository.lockOpen()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(
                        SALE_ID,
                        new CancelSaleCommand(PaymentMethod.CASH, "Sin caja"),
                        ACTOR_ID,
                        "Administración"))
                .isInstanceOf(ApplicationException.class);
        verify(saleRepository, never()).saveCancellation(any());
    }

    private Sale sale(SaleStatus status, SaleCancellation cancellation) {
        Money total = Money.of(new BigDecimal("406"));
        SaleLine line = new SaleLine(
                UUID.randomUUID(),
                PRODUCT_ID,
                "Producto",
                "001",
                2,
                Money.of(new BigDecimal("203")),
                new BigDecimal("100"),
                total);
        Payment payment = new Payment(
                UUID.randomUUID(),
                PaymentMethod.CASH,
                total,
                Money.of(new BigDecimal("410")),
                new BigDecimal("4"),
                Money.of(new BigDecimal("1000")),
                Money.of(new BigDecimal("590")),
                NOW);
        return new Sale(
                SALE_ID,
                1,
                CASH_ID,
                ACTOR_ID,
                "Administración",
                NOW,
                status,
                total,
                UUID.randomUUID(),
                "0".repeat(64),
                List.of(line),
                payment,
                cancellation);
    }

    private SaleCancellation cancellation() {
        return new SaleCancellation(
                UUID.randomUUID(),
                SALE_ID,
                PaymentMethod.CARD,
                Money.of(new BigDecimal("406")),
                null,
                null,
                "Anterior",
                ACTOR_ID,
                "Administración",
                NOW);
    }

    private CashSession cashSession() {
        return new CashSession(
                CASH_ID,
                ACTOR_ID,
                "Administración",
                NOW,
                Money.of(new BigDecimal("10000")),
                CashSessionStatus.OPEN);
    }
}
