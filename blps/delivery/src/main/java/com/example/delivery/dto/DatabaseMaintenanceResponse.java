package com.example.delivery.dto;

public class DatabaseMaintenanceResponse {
	private final String message;

	public DatabaseMaintenanceResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
