package com.example.delivery.dto;

import java.util.List;

public class CartResponse {
	private List<CartItemResponse> items;

	public CartResponse(List<CartItemResponse> items) {
		this.items = items;
	}

	public List<CartItemResponse> getItems() {
		return items;
	}
}
