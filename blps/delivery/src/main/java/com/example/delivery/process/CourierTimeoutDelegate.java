package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.service.CourierAssignmentService;

@Component("courierTimeoutDelegate")
public class CourierTimeoutDelegate implements JavaDelegate {
    private final CourierAssignmentService courierAssignmentService;

    public CourierTimeoutDelegate(CourierAssignmentService courierAssignmentService) {
        this.courierAssignmentService = courierAssignmentService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long orderId = ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
        courierAssignmentService.cancelBecauseNoCourier(orderId);
        execution.setVariable("orderResultSummary",
                "Order #" + orderId + " was automatically cancelled because no courier was found in 10 minutes");
    }
}
