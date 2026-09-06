package com.example.delivery.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shop_id", nullable = false)
	private User shop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "courier_id")
	private User courier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String deliveryAddress;

	private OffsetDateTime deliveryTime;

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	private OffsetDateTime shopRespondedAt;

	private OffsetDateTime acceptedAt;

	private OffsetDateTime cancelledAt;

	private OffsetDateTime courierAssignedAt;

	@Column(name = "process_instance_id", unique = true)
	private String processInstanceId;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	public Order() {
	}

	@PrePersist
	public void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		if (this.updatedAt == null) {
			this.updatedAt = this.createdAt;
		}
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public User getCustomer() {
		return customer;
	}

	public void setCustomer(User customer) {
		this.customer = customer;
	}

	public User getShop() {
		return shop;
	}

	public void setShop(User shop) {
		this.shop = shop;
	}

	public User getCourier() {
		return courier;
	}

	public void setCourier(User courier) {
		this.courier = courier;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

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

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public OffsetDateTime getShopRespondedAt() {
		return shopRespondedAt;
	}

	public void setShopRespondedAt(OffsetDateTime shopRespondedAt) {
		this.shopRespondedAt = shopRespondedAt;
	}

	public OffsetDateTime getAcceptedAt() {
		return acceptedAt;
	}

	public void setAcceptedAt(OffsetDateTime acceptedAt) {
		this.acceptedAt = acceptedAt;
	}

	public OffsetDateTime getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(OffsetDateTime cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public OffsetDateTime getCourierAssignedAt() {
		return courierAssignedAt;
	}

	public void setCourierAssignedAt(OffsetDateTime courierAssignedAt) {
		this.courierAssignedAt = courierAssignedAt;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public void setItems(List<OrderItem> items) {
		this.items = items;
	}
}
