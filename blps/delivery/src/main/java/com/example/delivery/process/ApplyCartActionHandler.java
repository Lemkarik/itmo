package com.example.delivery.process;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.dto.AddCartItemRequest;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.exception.ConflictException;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.service.CartService;

@Component
public class ApplyCartActionHandler implements DeliveryExternalTaskHandler {

    private final CartService cartService;
    private final CartProcessSynchronizationService cartProcessSynchronizationService;
    private final Clock clock;

    public ApplyCartActionHandler(
            CartService cartService,
            CartProcessSynchronizationService cartProcessSynchronizationService,
            Clock clock) {
        this.cartService = cartService;
        this.cartProcessSynchronizationService = cartProcessSynchronizationService;
        this.clock = clock;
    }

    @Override
    public String topic() {
        return "delivery-apply-cart-action";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        try {
            Long customerId = ExternalTaskVariables.requiredPositiveLong(task, "customerId");
            String action = ExternalTaskVariables.requiredString(task, "cartAction").toUpperCase();
            return switch (action) {
                case "ADD" -> addItem(task, customerId);
                case "REMOVE" -> removeItem(task, customerId);
                case "CHECKOUT" -> checkout(task, customerId);
                default -> throw new BadRequestException("Unsupported cart action: " + action);
            };
        } catch (BadRequestException | NotFoundException | ConflictException exception) {
            return rejectCheckout(exception.getMessage());
        }
    }

    private Map<String, Object> addItem(ExternalTask task, Long customerId) {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setShopItemId(ExternalTaskVariables.requiredPositiveLong(task, "shopItemId"));
        request.setQuantity(ExternalTaskVariables.requiredPositiveInteger(task, "quantity"));
        return refreshCart(task, customerId, cartService.addItem(customerId, request));
    }

    private Map<String, Object> removeItem(ExternalTask task, Long customerId) {
        return refreshCart(task, customerId, cartService.removeItem(
                customerId, ExternalTaskVariables.requiredPositiveLong(task, "removeShopItemId")));
    }

    private Map<String, Object> checkout(ExternalTask task, Long customerId) {
        CartResponse cart = cartService.getCart(customerId);
        if (cart.getItems().isEmpty()) {
            return rejectCheckout("Cart is empty");
        }
        Object address = task.getVariable("deliveryAddress");
        if (!(address instanceof String value) || value.isBlank()) {
            return rejectCheckout("Delivery address is required for checkout");
        }
        OffsetDateTime deliveryTime = ExternalTaskVariables.dateTime(task, "requestedDeliveryTime");
        if (deliveryTime == null) {
            return rejectCheckout("Delivery date and time are required for checkout");
        }
        if (!deliveryTime.isAfter(OffsetDateTime.now(clock))) {
            return rejectCheckout("Delivery date and time must be in the future");
        }
        return Map.of("checkoutRequested", true, "checkoutError", "");
    }

    private Map<String, Object> rejectCheckout(String message) {
        return Map.of("checkoutRequested", false, "checkoutError", message);
    }

    private Map<String, Object> refreshCart(ExternalTask task, Long customerId, CartResponse cart) {
        String cartSummary = InitializeShoppingHandler.cartSummary(cart);
        cartProcessSynchronizationService.synchronize(
                customerId, cartSummary, task.getProcessInstanceId());
        return Map.of(
                "cartSummary", cartSummary,
                "checkoutRequested", false,
                "checkoutError", "");
    }
}
