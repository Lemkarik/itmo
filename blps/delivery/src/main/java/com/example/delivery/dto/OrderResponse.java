package com.example.delivery.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.delivery.model.OrderStatus;

public class OrderResponse {
	private Long id;
	private OrderStatus status;
	private Long customerId;
	private Long shopId;
	private Long courierId;
	private String deliveryAddress;
	private OffsetDateTime requestedDeliveryTime;
	private Long estimatedDeliveryMinutes;
	private OffsetDateTime deliveryTime;
	private Double deliveryDistanceKm;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private OffsetDateTime shopRespondedAt;
	private OffsetDateTime acceptedAt;
	private OffsetDateTime cancelledAt;
	private OffsetDateTime courierAssignedAt;
	private List<OrderItemResponse> items;

	public OrderResponse(
			Long id,
			OrderStatus status,
			Long customerId,
			Long shopId,
			Long courierId,
			String deliveryAddress,
			OffsetDateTime requestedDeliveryTime,
			Long estimatedDeliveryMinutes,
			OffsetDateTime deliveryTime,
			Double deliveryDistanceKm,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt,
			OffsetDateTime shopRespondedAt,
			OffsetDateTime acceptedAt,
			OffsetDateTime cancelledAt,
			OffsetDateTime courierAssignedAt,
			List<OrderItemResponse> items
	) {
		this.id = id;
		this.status = status;
		this.customerId = customerId;
		this.shopId = shopId;
		this.courierId = courierId;
		this.deliveryAddress = deliveryAddress;
		this.requestedDeliveryTime = requestedDeliveryTime;
		this.estimatedDeliveryMinutes = estimatedDeliveryMinutes;
		this.deliveryTime = deliveryTime;
		this.deliveryDistanceKm = deliveryDistanceKm;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.shopRespondedAt = shopRespondedAt;
		this.acceptedAt = acceptedAt;
		this.cancelledAt = cancelledAt;
		this.courierAssignedAt = courierAssignedAt;
		this.items = items;
	}

	public Long getId() {
		return id;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public Long getShopId() {
		return shopId;
	}

	public Long getCourierId() {
		return courierId;
	}

	public String getDeliveryAddress() {
		return deliveryAddress;
	}

	public OffsetDateTime getRequestedDeliveryTime() {
		return requestedDeliveryTime;
	}

	public Long getEstimatedDeliveryMinutes() {
		return estimatedDeliveryMinutes;
	}

	public OffsetDateTime getDeliveryTime() {
		return deliveryTime;
	}

	public Double getDeliveryDistanceKm() {
		return deliveryDistanceKm;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getShopRespondedAt() {
		return shopRespondedAt;
	}

	public OffsetDateTime getAcceptedAt() {
		return acceptedAt;
	}

	public OffsetDateTime getCancelledAt() {
		return cancelledAt;
	}

	public OffsetDateTime getCourierAssignedAt() {
		return courierAssignedAt;
	}

	public List<OrderItemResponse> getItems() {
		return items;
	}
}
