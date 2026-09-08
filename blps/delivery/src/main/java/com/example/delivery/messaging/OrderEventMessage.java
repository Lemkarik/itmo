package com.example.delivery.messaging;

public class OrderEventMessage {

    private Long orderId;
    private Long customerId;
    private String message;

    public OrderEventMessage() {
    }

    public OrderEventMessage(Long orderId, Long customerId, String message) {
        this.orderId = orderId;
        this.customerId = customerId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
