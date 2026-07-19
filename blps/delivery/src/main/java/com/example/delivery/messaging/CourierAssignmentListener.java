package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.service.CourierAssignmentService;


@Component
@ConditionalOnProperty(name = "app.features.listener.courier-assignment", havingValue = "true", matchIfMissing = true)
public class CourierAssignmentListener {

    private static final Logger log = LoggerFactory.getLogger(CourierAssignmentListener.class);

    private final OrderRepository orderRepository;
    private final CourierAssignmentService courierAssignmentService;

    public CourierAssignmentListener(
            OrderRepository orderRepository,
            CourierAssignmentService courierAssignmentService) {
        this.orderRepository = orderRepository;
        this.courierAssignmentService = courierAssignmentService;
    }

    @JmsListener(destination = "courier.assignment", concurrency = "1-3")
    @Transactional
    public void handleCourierAssignment(OrderEventMessage message) {
        log.info("Received courier.assignment message for order {}",
                message.getOrderId());

        try {
            Order order = orderRepository.findById(message.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order {} not found for courier assignment",
                        message.getOrderId());
                return;
            }

            if (order.getStatus() != OrderStatus.ACCEPTED_WAITING_COURIER) {
                log.info("Order {} is not waiting for courier (status: {}), skipping",
                        order.getId(), order.getStatus());
                return;
            }

            boolean assigned = courierAssignmentService.tryAssignCourier(order);
            if (assigned) {
                log.info("Courier assigned to order {} successfully", order.getId());
            } else {
                log.info("No courier available for order {} at this time, will retry later",
                        order.getId());
            }
        } catch (Exception e) {
            log.error("Error processing courier assignment for order {}: {}",
                    message.getOrderId(), e.getMessage(), e);
        }
    }
}
