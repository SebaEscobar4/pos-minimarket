package com.minimarket.pos.inventory.infrastructure.persistence;

import com.minimarket.pos.inventory.application.InventoryBalanceInitializer;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInventoryBalanceInitializer implements InventoryBalanceInitializer {

    private final JdbcTemplate jdbcTemplate;

    public JdbcInventoryBalanceInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void initializeAtZero(UUID productId) {
        jdbcTemplate.update(
                "INSERT INTO inventory_balance (product_id, quantity, version) VALUES (?, 0, 0)",
                productId);
    }
}
