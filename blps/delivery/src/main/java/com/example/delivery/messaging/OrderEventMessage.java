package com.example.delivery.messaging;

import java.time.OffsetDateTime;

import com.example.delivery.model.OrderStatus;

/**
 * Message sent to queues when an order event occurs.
 * Used for async courier assignment, notification dispatch,
 * and external logistics integration.
 */
public class OrderEventMessage {

    private Long orderId;
    private Long customerId;
    private Long shopId;
    private Long courierId;
    private OrderStatus status;
    private String deliveryAddress;
    private OffsetDateTime eventTime;
    private String message;

    public OrderEventMessage() {
    }

    public OrderEventMessage(Long orderId, Long customerId, Long shopId,
                              OrderStatus status, String deliveryAddress,
                              String message) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.shopId = shopId;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.eventTime = OffsetDateTime.now();
        this.message = message;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getCourierId() {
        return courierId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
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

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(OffsetDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
