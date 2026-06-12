package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.DatabaseMaintenanceResponse;
import com.example.delivery.service.DatabaseMaintenanceService;

@RestController
@RequestMapping("/api/testing/database")
public class TestingController {

	private final DatabaseMaintenanceService databaseMaintenanceService;

	public TestingController(DatabaseMaintenanceService databaseMaintenanceService) {
		this.databaseMaintenanceService = databaseMaintenanceService;
	}

	@PostMapping("/clear")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_DATABASE')")
	public DatabaseMaintenanceResponse clearDatabase() {
		return databaseMaintenanceService.clearDatabase();
	}
}
