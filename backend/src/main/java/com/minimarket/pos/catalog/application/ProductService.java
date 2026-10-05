package com.minimarket.pos.catalog.application;

import com.minimarket.pos.catalog.domain.Category;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductInputRules;
import com.minimarket.pos.catalog.domain.ProductStatus;
import com.minimarket.pos.inventory.application.InventoryBalanceInitializer;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryBalanceInitializer balanceInitializer;
    private final Clock clock;

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            InventoryBalanceInitializer balanceInitializer) {
        this(productRepository, categoryRepository, balanceInitializer, Clock.systemUTC());
    }

    ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            InventoryBalanceInitializer balanceInitializer,
            Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.balanceInitializer = balanceInitializer;
        this.clock = clock;
    }

    @Transactional
    public Product create(ProductCommand command, UUID actorId) {
        ValidatedProduct validated = validate(command, null);
        Instant now = clock.instant();
        Product product = new Product(
                UUID.randomUUID(),
                validated.code(),
                validated.name(),
                validated.category().id(),
                validated.category().name(),
                validated.purchasePrice(),
                validated.salePrice(),
                validated.minimumStock(),
                ProductStatus.ACTIVE,
                0,
                now,
                now);
        try {
            productRepository.create(product, validated.searchName(), actorId);
        } catch (DuplicateKeyException exception) {
            throw duplicatedCode();
        }
        balanceInitializer.initializeAtZero(product.id());
        return product;
    }

    @Transactional
    public Product edit(UUID id, ProductCommand command, UUID actorId) {
        Product current = get(id);
        ValidatedProduct validated = validate(command, id);
        Product updated = new Product(
                current.id(),
                validated.code(),
                validated.name(),
                validated.category().id(),
                validated.category().name(),
                validated.purchasePrice(),
                validated.salePrice(),
                validated.minimumStock(),
                current.status(),
                current.currentStock(),
                current.createdAt(),
                clock.instant());
        try {
            if (!productRepository.update(updated, validated.searchName(), actorId)) {
                throw productNotFound();
            }
        } catch (DuplicateKeyException exception) {
            throw duplicatedCode();
        }
        return get(id);
    }

    @Transactional
    public Product changeStatus(UUID id, ProductStatus status, UUID actorId) {
        if (status == null) {
            throw validation("El estado del producto es obligatorio.");
        }
        if (!productRepository.updateStatus(id, status, actorId)) {
            throw productNotFound();
        }
        return get(id);
    }

    @Transactional(readOnly = true)
    public Product findActiveByCode(String code) {
        String normalized = ProductInputRules.normalizeCode(code);
        if (normalized == null) {
            throw validation("El código de búsqueda es obligatorio.");
        }
        return productRepository.findActiveByCode(normalized).orElseThrow(ProductService::productNotFound);
    }

    @Transactional(readOnly = true)
    public PageResult<Product> searchForPointOfSale(String name, int page, int size) {
        ProductInputRules.validatePage(page, size);
        return productRepository.search(ProductInputRules.searchName(name), false, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult<Product> searchForAdministration(
            String name, boolean includeInactive, int page, int size) {
        ProductInputRules.validatePage(page, size);
        String searchName = name == null || name.isBlank() ? null : ProductInputRules.searchName(name);
        return productRepository.search(searchName, includeInactive, page, size);
    }

    private ValidatedProduct validate(ProductCommand command, UUID excludedProductId) {
        if (command == null || command.categoryId() == null) {
            throw validation("La categoría del producto es obligatoria.");
        }
        String code = ProductInputRules.normalizeCode(command.code());
        if (code != null && productRepository.codeExists(code, excludedProductId)) {
            throw duplicatedCode();
        }
        Category category = categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> validation("La categoría indicada no existe."));
        String name = ProductInputRules.normalizeName(command.name());
        return new ValidatedProduct(
                code,
                name,
                ProductInputRules.searchName(name),
                category,
                ProductInputRules.money("El precio de compra", command.purchasePrice()),
                ProductInputRules.money("El precio de venta", command.salePrice()),
                ProductInputRules.minimumStock(command.minimumStock()));
    }

    private Product get(UUID id) {
        return productRepository.findById(id).orElseThrow(ProductService::productNotFound);
    }

    private static ApplicationException duplicatedCode() {
        return new ApplicationException(ProblemType.CONFLICT, "Ya existe un producto con ese código.");
    }

    private static ApplicationException productNotFound() {
        return new ApplicationException(ProblemType.NOT_FOUND, "El producto solicitado no existe.");
    }

    private static ApplicationException validation(String message) {
        return new ApplicationException(ProblemType.VALIDATION, message);
    }

    private record ValidatedProduct(
            String code,
            String name,
            String searchName,
            Category category,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            int minimumStock) {}
}
