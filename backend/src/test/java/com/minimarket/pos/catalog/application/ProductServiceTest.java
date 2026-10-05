package com.minimarket.pos.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.CategoryStatus;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import com.minimarket.pos.inventory.application.InventoryBalanceInitializer;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
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
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T08:00:00Z");
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Mock
    private ProductRepository products;

    @Mock
    private CategoryRepository categories;

    @Mock
    private InventoryBalanceInitializer balances;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(
                products, categories, balances, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsProductAndInitializesExactlyOneZeroBalance() {
        when(categories.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));
        ArgumentCaptor<Product> captured = ArgumentCaptor.forClass(Product.class);

        Product created = service.create(command(" 00123 ", " Café Molido "), ACTOR_ID);

        verify(products).create(captured.capture(), eq("cafe molido"), eq(ACTOR_ID));
        verify(balances).initializeAtZero(created.id());
        assertThat(captured.getValue()).isEqualTo(created);
        assertThat(created.code()).isEqualTo("00123");
        assertThat(created.currentStock()).isZero();
        assertThat(created.available()).isFalse();
        assertThat(created.purchasePrice()).isEqualByComparingTo("1200.00");
    }

    @Test
    void rejectsDuplicateCodesAndDatabaseRaces() {
        when(products.codeExists("00123", null)).thenReturn(true);
        assertProblem(() -> service.create(command("00123", "Café"), ACTOR_ID), ProblemType.CONFLICT);

        when(products.codeExists("00123", null)).thenReturn(false);
        when(categories.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));
        doThrow(new DuplicateKeyException("race"))
                .when(products)
                .create(any(), any(), eq(ACTOR_ID));
        assertProblem(() -> service.create(command("00123", "Café"), ACTOR_ID), ProblemType.CONFLICT);
    }

    @Test
    void editsFieldsWithoutChangingStockOrStatus() {
        UUID id = UUID.randomUUID();
        Product current = product(id, "00123", "Café", ProductStatus.INACTIVE, 7);
        Product persisted = product(id, null, "Té verde", ProductStatus.INACTIVE, 7);
        when(products.findById(id)).thenReturn(Optional.of(current), Optional.of(persisted));
        when(categories.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));
        when(products.update(any(), eq("te verde"), eq(ACTOR_ID))).thenReturn(true);

        Product result = service.edit(id, command(null, "Té verde"), ACTOR_ID);

        ArgumentCaptor<Product> update = ArgumentCaptor.forClass(Product.class);
        verify(products).update(update.capture(), eq("te verde"), eq(ACTOR_ID));
        assertThat(update.getValue().currentStock()).isEqualTo(7);
        assertThat(update.getValue().status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(result).isEqualTo(persisted);
    }

    @Test
    void changesStatusAndRejectsMissingResources() {
        UUID id = UUID.randomUUID();
        Product inactive = product(id, null, "Té", ProductStatus.INACTIVE, 0);
        when(products.updateStatus(id, ProductStatus.INACTIVE, ACTOR_ID)).thenReturn(true);
        when(products.findById(id)).thenReturn(Optional.of(inactive));

        assertThat(service.changeStatus(id, ProductStatus.INACTIVE, ACTOR_ID)).isEqualTo(inactive);
        assertProblem(() -> service.changeStatus(id, null, ACTOR_ID), ProblemType.VALIDATION);

        UUID missing = UUID.randomUUID();
        when(products.updateStatus(missing, ProductStatus.ACTIVE, ACTOR_ID)).thenReturn(false);
        assertProblem(
                () -> service.changeStatus(missing, ProductStatus.ACTIVE, ACTOR_ID),
                ProblemType.NOT_FOUND);
        assertProblem(() -> service.edit(missing, command(null, "Té"), ACTOR_ID), ProblemType.NOT_FOUND);
    }

    @Test
    void resolvesExactCodesAndBoundedNameSearches() {
        Product product = product(UUID.randomUUID(), "00123", "Café", ProductStatus.ACTIVE, 2);
        when(products.findActiveByCode("00123")).thenReturn(Optional.of(product));
        PageResult<Product> page = new PageResult<>(List.of(product), 0, 20, 1);
        when(products.search("cafe", false, 0, 20)).thenReturn(page);
        when(products.search(null, true, 0, 20)).thenReturn(page);

        assertThat(service.findActiveByCode(" 00123 ")).isEqualTo(product);
        assertThat(service.searchForPointOfSale("CAFÉ", 0, 20)).isEqualTo(page);
        assertThat(service.searchForAdministration(" ", true, 0, 20)).isEqualTo(page);
        assertProblem(() -> service.findActiveByCode(" "), ProblemType.VALIDATION);
        assertProblem(() -> service.findActiveByCode("unknown"), ProblemType.NOT_FOUND);
    }

    @Test
    void rejectsUnknownCategoryAndLostUpdate() {
        when(categories.findById(CATEGORY_ID)).thenReturn(Optional.empty());
        assertProblem(() -> service.create(command(null, "Té"), ACTOR_ID), ProblemType.VALIDATION);

        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.of(product(id, null, "Té", ProductStatus.ACTIVE, 0)));
        when(categories.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));
        when(products.update(any(), any(), eq(ACTOR_ID))).thenReturn(false);
        assertProblem(() -> service.edit(id, command(null, "Té"), ACTOR_ID), ProblemType.NOT_FOUND);
    }

    private ProductCommand command(String code, String name) {
        return new ProductCommand(
                code,
                name,
                CATEGORY_ID,
                new BigDecimal("1200"),
                new BigDecimal("1590"),
                3);
    }

    private Category category() {
        return new Category(CATEGORY_ID, "Abarrotes", CategoryStatus.ACTIVE, NOW, NOW);
    }

    private Product product(UUID id, String code, String name, ProductStatus status, int stock) {
        return new Product(
                id,
                code,
                name,
                CATEGORY_ID,
                "Abarrotes",
                new BigDecimal("1200.00"),
                new BigDecimal("1590.00"),
                3,
                status,
                stock,
                NOW,
                NOW);
    }

    private void assertProblem(Runnable action, ProblemType type) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).problemType())
                .isEqualTo(type);
    }
}
