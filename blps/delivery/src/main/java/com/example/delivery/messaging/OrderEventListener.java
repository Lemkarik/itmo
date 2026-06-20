package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * JMS listener for order events.
 * Processes messages from the "order.event" queue.
 * This simulates integration with an external system
 * that reacts to order lifecycle events.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @JmsListener(destination = "order.event", concurrency = "1-2")
    public void handleOrderEvent(OrderEventMessage message) {
        log.info("Received order.event: Order {} - Status: {} - {}",
                message.getOrderId(), message.getStatus(), message.getMessage());

        // Simulate processing of order event (e.g., analytics, monitoring)
        log.info("[EVENT PROCESSOR] Processing event for order {}: {}",
                message.getOrderId(), message.getMessage());

        // Here we could call external APIs, update analytics, etc.
        if (message.getStatus() != null) {
            switch (message.getStatus()) {
                case PENDING_SHOP -> log.info("[EVENT PROCESSOR] Order {} is pending shop decision",
                        message.getOrderId());
                case ACCEPTED_WAITING_COURIER -> log.info("[EVENT PROCESSOR] Order {} accepted by shop, waiting for courier",
                        message.getOrderId());
                case COURIER_ASSIGNED -> log.info("[EVENT PROCESSOR] Courier {} assigned to order {}",
                        message.getCourierId(), message.getOrderId());
                default -> log.info("[EVENT PROCESSOR] Order {} status changed to {}",
                        message.getOrderId(), message.getStatus());
            }
        }
    }
}
