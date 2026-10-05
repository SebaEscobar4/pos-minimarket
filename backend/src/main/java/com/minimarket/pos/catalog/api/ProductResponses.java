package com.minimarket.pos.catalog.api;

import com.minimarket.pos.catalog.application.PageResult;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class ProductResponses {

    private ProductResponses() {}

    static AdminProductResponse admin(Product product) {
        return new AdminProductResponse(
                product.id(),
                product.code(),
                product.name(),
                product.categoryId(),
                product.categoryName(),
                product.purchasePrice(),
                product.salePrice(),
                product.minimumStock(),
                product.status(),
                product.currentStock(),
                product.available(),
                product.createdAt(),
                product.updatedAt());
    }

    static SaleProductResponse sale(Product product) {
        return new SaleProductResponse(
                product.id(),
                product.code(),
                product.name(),
                product.categoryId(),
                product.categoryName(),
                product.salePrice(),
                product.currentStock(),
                product.available());
    }

    static <T> PageResponse<T> page(PageResult<Product> page, List<T> items) {
        return new PageResponse<>(
                items, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    record AdminProductResponse(
            UUID id,
            String code,
            String name,
            UUID categoryId,
            String categoryName,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            int minimumStock,
            ProductStatus status,
            int currentStock,
            boolean available,
            Instant createdAt,
            Instant updatedAt) {}

    record SaleProductResponse(
            UUID id,
            String code,
            String name,
            UUID categoryId,
            String categoryName,
            BigDecimal salePrice,
            int currentStock,
            boolean available) {}

    record PageResponse<T>(List<T> items, int page, int size, long totalElements, long totalPages) {}
}
