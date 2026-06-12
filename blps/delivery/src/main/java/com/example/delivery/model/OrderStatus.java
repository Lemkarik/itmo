package com.example.delivery.model;

public enum OrderStatus {
	PENDING_SHOP,
	DECLINED_BY_SHOP,
	ACCEPTED_WAITING_COURIER,
	CANCELLED_NO_COURIER,
	COURIER_ASSIGNED
}
