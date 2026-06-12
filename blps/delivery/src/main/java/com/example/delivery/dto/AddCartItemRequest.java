package com.example.delivery.dto;

public class AddCartItemRequest {
	private Long shopItemId;
	private Integer quantity;

	public Long getShopItemId() {
		return shopItemId;
	}

	public void setShopItemId(Long shopItemId) {
		this.shopItemId = shopItemId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
