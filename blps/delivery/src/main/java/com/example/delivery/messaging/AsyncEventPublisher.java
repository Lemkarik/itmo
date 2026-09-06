package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.Order;


@Service
public class AsyncEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventPublisher.class);

    private final MessageProducerService messageProducer;

    public AsyncEventPublisher(
            MessageProducerService messageProducer) {
        this.messageProducer = messageProducer;
    }

    
    @Transactional
    public void orderCreated(Order order) {
        submitToLogistics(order, "ORDER_CREATED");
    }

    
    @Transactional
    public void orderAccepted(Order order) {
        submitToLogistics(order, "ORDER_ACCEPTED");
    }

    
    @Transactional
    public void orderDeclined(Order order) {
        submitToLogistics(order, "ORDER_DECLINED");
    }

    
    @Transactional
    public void courierAssigned(Order order) {
        submitToLogistics(order, "COURIER_ASSIGNED");
    }

    
    @Transactional
    public void orderTimedOut(Order order) {
        submitToLogistics(order, "ORDER_TIMEOUT");
    }

    
    @Transactional
    public void courierNotFound(Order order) {
        submitToLogistics(order, "COURIER_NOT_FOUND");
    }

    
    private void submitToLogistics(Order order, String eventType) {
        try {
            messageProducer.sendExternalLogisticsEvent(order, eventType);
        } catch (Exception e) {
            log.warn("Failed to enqueue external.logistics event for order {}: {}",
                    order.getId(), e.getMessage());
        }
    }
}
