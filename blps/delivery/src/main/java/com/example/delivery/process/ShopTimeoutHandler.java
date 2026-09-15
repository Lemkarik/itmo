package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.model.Order;
import com.example.delivery.service.ShopService;

@Component
public class ShopTimeoutHandler implements DeliveryExternalTaskHandler {

    private final ShopService shopService;

    public ShopTimeoutHandler(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public String topic() {
        return "delivery-shop-timeout";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        Order order = shopService.timeoutShopDecision(
                ExternalTaskVariables.requiredLong(task, DeliveryProcessService.ORDER_ID));
        return Map.of("orderResultSummary",
                "Order #" + order.getId()
                        + " was automatically cancelled because the shop did not respond in 10 minutes");
    }
}
