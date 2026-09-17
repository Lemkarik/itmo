package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.service.OrderService;

@Component
public class CreateOrderHandler implements DeliveryExternalTaskHandler {

    private final OrderService orderService;
    private final CartProcessSynchronizationService cartProcessSynchronizationService;

    public CreateOrderHandler(
            OrderService orderService,
            CartProcessSynchronizationService cartProcessSynchronizationService) {
        this.orderService = orderService;
        this.cartProcessSynchronizationService = cartProcessSynchronizationService;
    }

    @Override
    public String topic() {
        return "delivery-create-order";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        Long customerId = ExternalTaskVariables.requiredLong(task, "customerId");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDeliveryAddress(task.getVariable("deliveryAddress"));
        request.setDeliveryTime(ExternalTaskVariables.dateTime(task, "requestedDeliveryTime"));
        OrderResponse order = orderService.createOrder(
                customerId, request, task.getProcessInstanceId());
        cartProcessSynchronizationService.synchronize(
                customerId, InitializeShoppingHandler.EMPTY_CART_SUMMARY, task.getProcessInstanceId());

        return Map.of(
                DeliveryProcessService.ORDER_ID, order.getId(),
                "cartSummary", InitializeShoppingHandler.EMPTY_CART_SUMMARY,
                "orderNumber", order.getId().toString(),
                "orderSummary", orderSummary(order));
    }

    private String orderSummary(OrderResponse order) {
        StringBuilder summary = new StringBuilder()
                .append("Order #").append(order.getId())
                .append("\nAddress: ").append(order.getDeliveryAddress())
                .append("\nDelivery time: ").append(order.getDeliveryTime())
                .append("\nItems:");
        order.getItems().forEach(item -> summary
                .append("\n- ").append(item.getProductName())
                .append(" x ").append(item.getQuantity())
                .append(" @ ").append(item.getUnitPrice()));
        return summary.toString();
    }
}
