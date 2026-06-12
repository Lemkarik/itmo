package com.example.delivery.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
	CUSTOMER,
	SHOP,
	COURIER,
	ADMIN;

	@JsonCreator
	public static UserRole fromString(String value) {
		if (value == null) {
			return null;
		}
		return UserRole.valueOf(value.toUpperCase());
	}

	@JsonValue
	public String toValue() {
		return name();
	}
}
