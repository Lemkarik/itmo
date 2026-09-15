package com.example.delivery.process;

import java.util.LinkedHashMap;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.model.Order;
import com.example.delivery.service.CourierAssignmentService;
import com.example.delivery.service.OrderService;

@Component
public class CourierAssignedHandler implements DeliveryExternalTaskHandler {

    private final CourierAssignmentService courierAssignmentService;
    private final OrderService orderService;

    public CourierAssignedHandler(
            CourierAssignmentService courierAssignmentService,
            OrderService orderService) {
        this.courierAssignmentService = courierAssignmentService;
        this.orderService = orderService;
    }

    @Override
    public String topic() {
        return "delivery-courier-assigned";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        Long orderId = ExternalTaskVariables.requiredLong(task, DeliveryProcessService.ORDER_ID);
        courierAssignmentService.notifyCourierAssigned(orderId);
        Order order = orderService.getOrderEntity(orderId);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("customerUsername", order.getCustomer().getUsername());
        variables.put("courierUsername", order.getCourier().getUsername());
        variables.put("assignmentSummary",
                "Order #" + order.getId()
                        + "\nCourier: " + order.getCourier().getName()
                        + "\nDelivery address: " + order.getDeliveryAddress()
                        + "\nDelivery time: " + order.getDeliveryTime());
        return variables;
    }
}
