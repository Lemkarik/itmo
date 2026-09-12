package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.dto.OrderDecisionRequest;
import com.example.delivery.model.Order;
import com.example.delivery.service.ShopService;

@Component("shopDeclinedDelegate")
public class ShopDeclinedDelegate implements JavaDelegate {
    private final ShopService shopService;

    public ShopDeclinedDelegate(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        OrderDecisionRequest request = new OrderDecisionRequest();
        request.setReason((String) execution.getVariable("declineReason"));
        Order order = shopService.declineOrder(orderId(execution), request);
        execution.setVariable("orderResultSummary",
                "Order #" + order.getId() + " was declined by the shop"
                        + (request.getReason() == null || request.getReason().isBlank()
                                ? ""
                                : ". Reason: " + request.getReason()));
    }

    private Long orderId(DelegateExecution execution) {
        return ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
    }
}
