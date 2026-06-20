package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;


@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @JmsListener(destination = "order.event", concurrency = "1-2")
    public void handleOrderEvent(OrderEventMessage message) {
        log.info("Received order.event: Order {} - Status: {} - {}",
                message.getOrderId(), message.getStatus(), message.getMessage());

        
        log.info("[EVENT PROCESSOR] Processing event for order {}: {}",
                message.getOrderId(), message.getMessage());

        
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
