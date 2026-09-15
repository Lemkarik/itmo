package com.example.delivery.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.DatabaseMaintenanceResponse;
import com.example.delivery.model.Privilege;

@Service
public class DatabaseMaintenanceService {

    private static final String TRUNCATE_ALL_TABLES = """
            TRUNCATE TABLE processed_external_tasks, notifications, order_items, orders,
                cart_items, carts, shop_items, users
            RESTART IDENTITY CASCADE
            """;

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DatabaseMaintenanceResponse clearDatabase() {
        jdbcTemplate.execute(TRUNCATE_ALL_TABLES);
        return new DatabaseMaintenanceResponse("Database cleared");
    }
}
