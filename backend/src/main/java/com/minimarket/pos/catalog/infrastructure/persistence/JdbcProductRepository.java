package com.minimarket.pos.catalog.infrastructure.persistence;

import com.minimarket.pos.catalog.application.PageResult;
import com.minimarket.pos.catalog.application.ProductRepository;
import com.minimarket.pos.catalog.domain.Product;
import com.minimarket.pos.catalog.domain.ProductStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProductRepository implements ProductRepository {

    private static final String SELECT_PRODUCT = """
            SELECT p.id, p.code, p.name, p.category_id, c.name AS category_name,
                   p.purchase_price, p.sale_price, p.minimum_stock, p.status,
                   b.quantity AS current_stock, p.created_at, p.updated_at
            FROM catalog_product p
            JOIN catalog_category c ON c.id = p.category_id
            JOIN inventory_balance b ON b.product_id = p.id
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(Product product, String searchName, UUID actorId) {
        jdbcTemplate.update(
                """
                INSERT INTO catalog_product (
                    id, code, name, search_name, category_id, purchase_price, sale_price,
                    minimum_stock, status, created_at, updated_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                product.id(),
                product.code(),
                product.name(),
                searchName,
                product.categoryId(),
                product.purchasePrice(),
                product.salePrice(),
                product.minimumStock(),
                product.status().name(),
                Timestamp.from(product.createdAt()),
                Timestamp.from(product.updatedAt()),
                actorId,
                actorId);
    }

    @Override
    public boolean update(Product product, String searchName, UUID actorId) {
        return jdbcTemplate.update(
                        """
                        UPDATE catalog_product
                        SET code = ?, name = ?, search_name = ?, category_id = ?,
                            purchase_price = ?, sale_price = ?, minimum_stock = ?,
                            updated_at = CURRENT_TIMESTAMP, updated_by = ?
                        WHERE id = ?
                        """,
                        product.code(),
                        product.name(),
                        searchName,
                        product.categoryId(),
                        product.purchasePrice(),
                        product.salePrice(),
                        product.minimumStock(),
                        actorId,
                        product.id())
                == 1;
    }

    @Override
    public boolean updateStatus(UUID id, ProductStatus status, UUID actorId) {
        return jdbcTemplate.update(
                        """
                        UPDATE catalog_product
                        SET status = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
                        WHERE id = ?
                        """,
                        status.name(),
                        actorId,
                        id)
                == 1;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jdbcTemplate.query(SELECT_PRODUCT + " WHERE p.id = ?", this::mapProduct, id).stream()
                .findFirst();
    }

    @Override
    public Optional<Product> findActiveByCode(String code) {
        return jdbcTemplate
                .query(
                        SELECT_PRODUCT + " WHERE p.status = 'ACTIVE' AND p.code = ?",
                        this::mapProduct,
                        code)
                .stream()
                .findFirst();
    }

    @Override
    public boolean codeExists(String code, UUID excludedProductId) {
        Long count;
        if (excludedProductId == null) {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM catalog_product WHERE code = ?", Long.class, code);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM catalog_product WHERE code = ? AND id <> ?",
                    Long.class,
                    code,
                    excludedProductId);
        }
        return count != null && count > 0;
    }

    @Override
    public PageResult<Product> search(
            String searchName, boolean includeInactive, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (!includeInactive) {
            where.append(" AND p.status = 'ACTIVE'");
        }
        if (searchName != null) {
            where.append(" AND p.search_name LIKE ? ESCAPE '\\'");
            parameters.add("%" + escapeLike(searchName) + "%");
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM catalog_product p" + where,
                Long.class,
                parameters.toArray());
        parameters.add(size);
        parameters.add((long) page * size);
        List<Product> products = jdbcTemplate.query(
                SELECT_PRODUCT + where + " ORDER BY p.search_name, p.id LIMIT ? OFFSET ?",
                this::mapProduct,
                parameters.toArray());
        return new PageResult<>(products, page, size, total == null ? 0 : total);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Product mapProduct(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Product(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getObject("category_id", UUID.class),
                resultSet.getString("category_name"),
                resultSet.getBigDecimal("purchase_price"),
                resultSet.getBigDecimal("sale_price"),
                resultSet.getInt("minimum_stock"),
                ProductStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("current_stock"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
