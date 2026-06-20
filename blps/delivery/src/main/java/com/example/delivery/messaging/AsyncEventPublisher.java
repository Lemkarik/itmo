package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.jca.LogisticsEISService;
import com.example.delivery.jca.LogisticsException;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;


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

    
    @Transactional
    public void orderCreated(Order order) {
        messageProducer.sendOrderEvent(order, "ORDER_CREATED");
        
        
        submitToLogistics(order, "ORDER_CREATED");
    }

    
    @Transactional
    public void orderAccepted(Order order) {
        messageProducer.sendCourierAssignmentRequest(order);
        messageProducer.sendOrderEvent(order, "ORDER_ACCEPTED");
        
        submitToLogistics(order, "ORDER_ACCEPTED");
    }

    
    @Transactional
    public void orderDeclined(Order order) {
        messageProducer.sendOrderEvent(order, "ORDER_DECLINED");
        
        submitToLogistics(order, "ORDER_DECLINED");
    }

    
    @Transactional
    public void courierAssigned(Order order) {
        messageProducer.sendOrderEvent(order, "COURIER_ASSIGNED");

        
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

    
    @Transactional
    public void orderTimedOut(Order order) {
        messageProducer.sendOrderEvent(order, "ORDER_TIMEOUT");
        
        submitToLogistics(order, "ORDER_TIMEOUT");
    }

    
    @Transactional
    public void courierNotFound(Order order) {
        messageProducer.sendOrderEvent(order, "COURIER_NOT_FOUND");
        
        submitToLogistics(order, "COURIER_NOT_FOUND");
    }

    
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
