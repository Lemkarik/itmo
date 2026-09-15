package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.messaging.MessageProducerService;
import com.example.delivery.service.OrderService;

@Component
public class DispatchCourierSearchHandler implements DeliveryExternalTaskHandler {

    private final OrderService orderService;
    private final MessageProducerService messageProducerService;

    public DispatchCourierSearchHandler(
            OrderService orderService,
            MessageProducerService messageProducerService) {
        this.orderService = orderService;
        this.messageProducerService = messageProducerService;
    }

    @Override
    public String topic() {
        return "delivery-dispatch-courier-search";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        Long orderId = ExternalTaskVariables.requiredLong(task, DeliveryProcessService.ORDER_ID);
        messageProducerService.sendCourierAssignmentRequest(orderService.getOrderEntity(orderId));
        return Map.of();
    }
}
