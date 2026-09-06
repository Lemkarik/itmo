package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.service.CourierAssignmentService;
import com.example.delivery.model.Order;
import com.example.delivery.service.OrderService;

@Component("courierAssignedDelegate")
public class CourierAssignedDelegate implements JavaDelegate {
    private final CourierAssignmentService courierAssignmentService;
    private final OrderService orderService;

    public CourierAssignedDelegate(
            CourierAssignmentService courierAssignmentService,
            OrderService orderService) {
        this.courierAssignmentService = courierAssignmentService;
        this.orderService = orderService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
        courierAssignmentService.notifyCourierAssigned(orderId);
        Order order = orderService.getOrderEntity(orderId);
        execution.setVariable("customerUsername", order.getCustomer().getUsername());
        execution.setVariable("shopUsername", order.getShop().getUsername());
        execution.setVariable("courierUsername", order.getCourier().getUsername());
        execution.setVariable("assignmentSummary",
                "Order #" + order.getId()
                        + "\nCourier: " + order.getCourier().getName()
                        + "\nDelivery address: " + order.getDeliveryAddress()
                        + "\nDelivery time: " + order.getDeliveryTime());
    }
}
