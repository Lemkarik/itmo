package com.example.delivery.dto;

import java.time.OffsetDateTime;

import com.example.delivery.model.NotificationType;

public class NotificationResponse {
	private Long id;
	private NotificationType type;
	private String message;
	private OffsetDateTime createdAt;
	private OffsetDateTime readAt;
	private Long orderId;

	public NotificationResponse(Long id, NotificationType type, String message, OffsetDateTime createdAt, OffsetDateTime readAt, Long orderId) {
		this.id = id;
		this.type = type;
		this.message = message;
		this.createdAt = createdAt;
		this.readAt = readAt;
		this.orderId = orderId;
	}

	public Long getId() {
		return id;
	}

	public NotificationType getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getReadAt() {
		return readAt;
	}

	public Long getOrderId() {
		return orderId;
	}
}
