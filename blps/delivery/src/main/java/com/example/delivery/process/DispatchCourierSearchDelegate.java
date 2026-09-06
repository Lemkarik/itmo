package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.messaging.MessageProducerService;
import com.example.delivery.service.OrderService;

@Component("dispatchCourierSearchDelegate")
public class DispatchCourierSearchDelegate implements JavaDelegate {
    private final OrderService orderService;
    private final MessageProducerService messageProducerService;

    public DispatchCourierSearchDelegate(OrderService orderService, MessageProducerService messageProducerService) {
        this.orderService = orderService;
        this.messageProducerService = messageProducerService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
        messageProducerService.sendCourierAssignmentRequest(orderService.getOrderEntity(orderId));
    }
}
