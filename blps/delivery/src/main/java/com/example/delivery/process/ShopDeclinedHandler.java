package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.dto.OrderDecisionRequest;
import com.example.delivery.model.Order;
import com.example.delivery.service.ShopService;

@Component
public class ShopDeclinedHandler implements DeliveryExternalTaskHandler {

    private final ShopService shopService;

    public ShopDeclinedHandler(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public String topic() {
        return "delivery-shop-declined";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        OrderDecisionRequest request = new OrderDecisionRequest();
        request.setReason(task.getVariable("declineReason"));
        Order order = shopService.declineOrder(
                ExternalTaskVariables.requiredLong(task, DeliveryProcessService.ORDER_ID), request);
        String reason = request.getReason();
        return Map.of("orderResultSummary",
                "Order #" + order.getId() + " was declined by the shop"
                        + (reason == null || reason.isBlank() ? "" : ". Reason: " + reason));
    }
}
