package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.service.ShopService;

@Component("shopAcceptedDelegate")
public class ShopAcceptedDelegate implements JavaDelegate {
    private final ShopService shopService;

    public ShopAcceptedDelegate(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        shopService.acceptOrder(orderId(execution));
    }

    private Long orderId(DelegateExecution execution) {
        return ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
    }
}
