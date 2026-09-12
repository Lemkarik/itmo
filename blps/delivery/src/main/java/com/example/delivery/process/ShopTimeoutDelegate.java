package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.model.Order;
import com.example.delivery.service.ShopService;

@Component("shopTimeoutDelegate")
public class ShopTimeoutDelegate implements JavaDelegate {
    private final ShopService shopService;

    public ShopTimeoutDelegate(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Order order = shopService.timeoutShopDecision(orderId(execution));
        execution.setVariable("orderResultSummary",
                "Order #" + order.getId() + " was automatically cancelled because the shop did not respond in 10 minutes");
    }

    private Long orderId(DelegateExecution execution) {
        return ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
    }
}
