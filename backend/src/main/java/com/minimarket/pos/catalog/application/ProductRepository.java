package com.minimarket.pos.catalog.application;

import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    void create(Product product, String searchName, UUID actorId);

    boolean update(Product product, String searchName, UUID actorId);

    boolean updateStatus(UUID id, ProductStatus status, UUID actorId);

    Optional<Product> findById(UUID id);

    Optional<Product> findActiveByCode(String code);

    boolean codeExists(String code, UUID excludedProductId);

    PageResult<Product> search(String searchName, boolean includeInactive, int page, int size);
}
