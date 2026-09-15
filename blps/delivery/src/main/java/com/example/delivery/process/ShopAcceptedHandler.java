package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.service.ShopService;

@Component
public class ShopAcceptedHandler implements DeliveryExternalTaskHandler {

    private final ShopService shopService;

    public ShopAcceptedHandler(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public String topic() {
        return "delivery-shop-accepted";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        shopService.acceptOrder(ExternalTaskVariables.requiredLong(task, DeliveryProcessService.ORDER_ID));
        return Map.of();
    }
}
