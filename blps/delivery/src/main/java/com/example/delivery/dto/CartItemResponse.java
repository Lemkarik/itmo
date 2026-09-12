package com.example.delivery.dto;

import java.math.BigDecimal;

public class CartItemResponse {
	private Long shopItemId;
	private String productName;
	private Integer quantity;
	private BigDecimal unitPrice;

	public CartItemResponse(Long shopItemId, String productName, Integer quantity, BigDecimal unitPrice) {
		this.shopItemId = shopItemId;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	public Long getShopItemId() {
		return shopItemId;
	}

	public String getProductName() {
		return productName;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}
}
