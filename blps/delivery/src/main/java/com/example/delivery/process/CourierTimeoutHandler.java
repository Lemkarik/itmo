package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.service.CourierAssignmentService;

@Component
public class CourierTimeoutHandler implements DeliveryExternalTaskHandler {

    private final CourierAssignmentService courierAssignmentService;

    public CourierTimeoutHandler(CourierAssignmentService courierAssignmentService) {
        this.courierAssignmentService = courierAssignmentService;
    }

    @Override
    public String topic() {
        return "delivery-courier-timeout";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        Long orderId = ExternalTaskVariables.requiredLong(task, DeliveryProcessService.ORDER_ID);
        courierAssignmentService.cancelBecauseNoCourier(orderId);
        return Map.of("orderResultSummary",
                "Order #" + orderId
                        + " was automatically cancelled because no courier was found in 10 minutes");
    }
}
