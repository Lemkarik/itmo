package com.example.delivery.dto;

import java.util.List;

public class CartResponse {
	private Long customerId;
	private List<CartItemResponse> items;

	public CartResponse(Long customerId, List<CartItemResponse> items) {
		this.customerId = customerId;
		this.items = items;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public List<CartItemResponse> getItems() {
		return items;
	}
}
