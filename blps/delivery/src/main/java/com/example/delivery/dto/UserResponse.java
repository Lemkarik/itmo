package com.example.delivery.dto;

import com.example.delivery.model.UserRole;

public class UserResponse {
	private Long id;
	private String name;
	private UserRole role;
	private Boolean available;

	public UserResponse(Long id, String name, UserRole role, Boolean available) {
		this.id = id;
		this.name = name;
		this.role = role;
		this.available = available;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public UserRole getRole() {
		return role;
	}

	public Boolean getAvailable() {
		return available;
	}
}
