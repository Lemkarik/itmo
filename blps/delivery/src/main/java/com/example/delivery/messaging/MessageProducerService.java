package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;


@Service
public class MessageProducerService {

    private static final Logger log = LoggerFactory.getLogger(MessageProducerService.class);

    private final JmsTemplate jmsTemplate;

    public MessageProducerService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    
    public void sendCourierAssignmentRequest(Order order) {
        OrderEventMessage msg = new OrderEventMessage(
                order.getId(),
                order.getCustomer().getId(),
                order.getShop().getId(),
                OrderStatus.ACCEPTED_WAITING_COURIER,
                order.getDeliveryAddress(),
                "New courier assignment request for order " + order.getId()
        );
        jmsTemplate.convertAndSend("courier.assignment", msg);
        log.info("Sent courier.assignment message for order {}", order.getId());
    }

    
    public void sendNotificationDispatch(Long recipientId, Long orderId,
                                          String type, String message) {
        OrderEventMessage msg = new OrderEventMessage(
                orderId, null, null, null, null, message
        );
        msg.setCustomerId(recipientId);
        jmsTemplate.convertAndSend("notification.dispatch", msg);
        log.info("Sent notification.dispatch message for order {} to recipient {}",
                orderId, recipientId);
    }

    
    public void sendOrderEvent(Order order, String eventType) {
        OrderEventMessage msg = new OrderEventMessage(
                order.getId(),
                order.getCustomer().getId(),
                order.getShop().getId(),
                order.getStatus(),
                order.getDeliveryAddress(),
                eventType + " for order " + order.getId()
        );
        if (order.getCourier() != null) {
            msg.setCourierId(order.getCourier().getId());
        }
        jmsTemplate.convertAndSend("order.event", msg);
        log.info("Sent order.event message for order {}: {}", order.getId(), eventType);
    }

    
    public void sendExternalLogisticsEvent(Order order, String eventDescription) {
        OrderEventMessage msg = new OrderEventMessage(
                order.getId(),
                order.getCustomer().getId(),
                order.getShop().getId(),
                order.getStatus(),
                order.getDeliveryAddress(),
                eventDescription
        );
        if (order.getCourier() != null) {
            msg.setCourierId(order.getCourier().getId());
        }
        jmsTemplate.convertAndSend("external.logistics", msg);
        log.info("Sent external.logistics message for order {}: {}",
                order.getId(), eventDescription);
    }
}
