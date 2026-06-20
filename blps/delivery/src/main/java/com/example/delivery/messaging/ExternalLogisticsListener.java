package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;


@Component
public class ExternalLogisticsListener {

    private static final Logger log = LoggerFactory.getLogger(ExternalLogisticsListener.class);

    @JmsListener(destination = "external.logistics", concurrency = "1-2")
    public void handleExternalLogistics(OrderEventMessage message) {
        log.info("=== EXTERNAL LOGISTICS SYSTEM ===");
        log.info("Received logistics event: Order {} - {}", 
                message.getOrderId(), message.getMessage());
        log.info("Customer ID: {}, Shop ID: {}, Courier ID: {}",
                message.getCustomerId(), message.getShopId(), message.getCourierId());
        log.info("Delivery address: {}", message.getDeliveryAddress());
        log.info("Event time: {}", message.getEventTime());

        
        
        try {
            
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[LOGISTICS] Order {} successfully sent to logistics provider for processing",
                message.getOrderId());
        log.info("=== END EXTERNAL LOGISTICS ===");
    }
}
