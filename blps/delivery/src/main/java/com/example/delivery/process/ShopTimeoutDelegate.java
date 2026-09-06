package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.messaging.AsyncEventPublisher;
import com.example.delivery.model.Order;
import com.example.delivery.service.ShopService;

@Component("shopTimeoutDelegate")
public class ShopTimeoutDelegate implements JavaDelegate {
    private final ShopService shopService;
    private final AsyncEventPublisher asyncEventPublisher;

    public ShopTimeoutDelegate(ShopService shopService, AsyncEventPublisher asyncEventPublisher) {
        this.shopService = shopService;
        this.asyncEventPublisher = asyncEventPublisher;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Order order = shopService.timeoutShopDecision(orderId(execution));
        asyncEventPublisher.orderTimedOut(order);
        execution.setVariable("orderResultSummary",
                "Order #" + order.getId() + " was automatically cancelled because the shop did not respond in 10 minutes");
    }

    private Long orderId(DelegateExecution execution) {
        return ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
    }
}
