package com.example.delivery.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.delivery.service.BaselineDataService;

@Component
public class DataInitializer implements CommandLineRunner {

	private final BaselineDataService baselineDataService;
	private final JdbcTemplate jdbcTemplate;

	public DataInitializer(BaselineDataService baselineDataService, JdbcTemplate jdbcTemplate) {
		this.baselineDataService = baselineDataService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(String... args) {
		jdbcTemplate.execute("ALTER TABLE IF EXISTS orders ALTER COLUMN delivery_address TYPE TEXT");
		jdbcTemplate.execute("ALTER TABLE IF EXISTS users DROP CONSTRAINT IF EXISTS users_role_check");
		jdbcTemplate.execute("""
				ALTER TABLE users
				ADD CONSTRAINT users_role_check
				CHECK (role IN ('CUSTOMER', 'SHOP', 'COURIER', 'ADMIN'))
				""");
		baselineDataService.ensureDefaultData();
	}
}
