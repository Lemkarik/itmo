package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.delivery.messaging.AsyncEventPublisher;
import com.example.delivery.model.Order;
import com.example.delivery.service.ShopService;

@Component("shopAcceptedDelegate")
public class ShopAcceptedDelegate implements JavaDelegate {
    private final ShopService shopService;
    private final AsyncEventPublisher asyncEventPublisher;

    public ShopAcceptedDelegate(ShopService shopService, AsyncEventPublisher asyncEventPublisher) {
        this.shopService = shopService;
        this.asyncEventPublisher = asyncEventPublisher;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Order order = shopService.acceptOrder(orderId(execution));
        asyncEventPublisher.orderAccepted(order);
    }

    private Long orderId(DelegateExecution execution) {
        return ((Number) execution.getVariable(DeliveryProcessService.ORDER_ID)).longValue();
    }
}
