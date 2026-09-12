package com.example.delivery.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.Order;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.process.DeliveryProcessService;

@Component
@ConditionalOnProperty(
        name = "app.features.listener.process-correlation",
        havingValue = "true",
        matchIfMissing = true)
public class CourierAssignedProcessListener {

    private final OrderRepository orderRepository;
    private final DeliveryProcessService deliveryProcessService;

    public CourierAssignedProcessListener(
            OrderRepository orderRepository,
            DeliveryProcessService deliveryProcessService) {
        this.orderRepository = orderRepository;
        this.deliveryProcessService = deliveryProcessService;
    }

    @JmsListener(
            destination = "process.courier-assigned",
            containerFactory = "transactionalJmsListenerContainerFactory")
    @Transactional
    public void correlateCourierAssigned(OrderEventMessage message) {
        Order order = orderRepository.findById(message.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "Order not found for process correlation: " + message.getOrderId()));
        deliveryProcessService.correlateCourierAssigned(order);
    }
}
