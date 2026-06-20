package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.jca.LogisticsEISService;
import com.example.delivery.jca.LogisticsException;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;

/**
 * Service that publishes events asynchronously via ActiveMQ queues
 * and integrates with the external Logistics EIS via JCA.
 * 
 * This decouples the core business logic from side effects
 * like courier assignment, notification dispatch, and external system integration.
 * 
 * Distributed transaction context: the JMS send operations and JCA calls
 * participate in the caller's JTA transaction (managed by Narayana).
 */
@Service
public class AsyncEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventPublisher.class);

    private final MessageProducerService messageProducer;
    private final LogisticsEISService logisticsEISService;

    public AsyncEventPublisher(
            MessageProducerService messageProducer,
            LogisticsEISService logisticsEISService) {
        this.messageProducer = messageProducer;
        this.logisticsEISService = logisticsEISService;
    }

    /**
     * Publish event when an order is created.
     * Sends async messages and submits order to logistics EIS via JCA.
     */
    @Transactional
    public void orderCreated(Order order) {
        messageProducer.sendOrderEvent(order, "ORDER_CREATED");
        
        // Submit order to external logistics system via JCA
        submitToLogistics(order, "ORDER_CREATED");
    }

    /**
     * Publish event when a shop accepts an order.
     * Triggers async courier assignment via queue and updates logistics.
     */
    @Transactional
    public void orderAccepted(Order order) {
        messageProducer.sendCourierAssignmentRequest(order);
        messageProducer.sendOrderEvent(order, "ORDER_ACCEPTED");
        
        submitToLogistics(order, "ORDER_ACCEPTED");
    }

    /**
     * Publish event when a shop declines an order.
     */
    @Transactional
    public void orderDeclined(Order order) {
        messageProducer.sendOrderEvent(order, "ORDER_DECLINED");
        
        submitToLogistics(order, "ORDER_DECLINED");
    }

    /**
     * Publish event when a courier is assigned.
     */
    @Transactional
    public void courierAssigned(Order order) {
        messageProducer.sendOrderEvent(order, "COURIER_ASSIGNED");

        // Send notifications asynchronously
        messageProducer.sendNotificationDispatch(
                order.getCustomer().getId(), order.getId(),
                "COURIER_ASSIGNED",
                "Courier assigned to order " + order.getId()
        );
        if (order.getCourier() != null) {
            messageProducer.sendNotificationDispatch(
                    order.getCourier().getId(), order.getId(),
                    "COURIER_ASSIGNED",
                    "You have been assigned to order " + order.getId()
            );
        }
        messageProducer.sendNotificationDispatch(
                order.getShop().getId(), order.getId(),
                "COURIER_ASSIGNED",
                "Courier assigned to order " + order.getId()
        );
        
        submitToLogistics(order, "COURIER_ASSIGNED");
    }

    /**
     * Publish event when an order times out (no shop response).
     */
    @Transactional
    public void orderTimedOut(Order order) {
        messageProducer.sendOrderEvent(order, "ORDER_TIMEOUT");
        
        submitToLogistics(order, "ORDER_TIMEOUT");
    }

    /**
     * Publish event when no courier is found.
     */
    @Transactional
    public void courierNotFound(Order order) {
        messageProducer.sendOrderEvent(order, "COURIER_NOT_FOUND");
        
        submitToLogistics(order, "COURIER_NOT_FOUND");
    }

    /**
     * Submit order event to the external logistics system via JCA.
     * Uses the LogisticsEISService which communicates through the
     * JCA resource adapter.
     */
    private void submitToLogistics(Order order, String eventType) {
        try {
            String trackingId = logisticsEISService.submitOrderToLogistics(order, eventType);
            log.info("Order {} submitted to logistics EIS via JCA. Tracking: {}",
                    order.getId(), trackingId);
        } catch (LogisticsException e) {
            log.warn("Failed to submit order {} to logistics EIS (will retry later): {}",
                    order.getId(), e.getMessage());
        }
    }
}
