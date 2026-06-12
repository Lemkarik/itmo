package com.example.delivery.dto;

import java.time.OffsetDateTime;

public class CreateOrderRequest {
	private String deliveryAddress;
	private OffsetDateTime deliveryTime;

	public String getDeliveryAddress() {
		return deliveryAddress;
	}

	public void setDeliveryAddress(String deliveryAddress) {
		this.deliveryAddress = deliveryAddress;
	}

	public OffsetDateTime getDeliveryTime() {
		return deliveryTime;
	}

	public void setDeliveryTime(OffsetDateTime deliveryTime) {
		this.deliveryTime = deliveryTime;
	}
}
