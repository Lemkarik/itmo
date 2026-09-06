package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.AddCartItemRequest;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.service.CartService;

@Component("applyCartActionDelegate")
public class ApplyCartActionDelegate implements JavaDelegate {

    private final CartService cartService;
    private final CartProcessSynchronizationService cartProcessSynchronizationService;

    public ApplyCartActionDelegate(
            CartService cartService,
            CartProcessSynchronizationService cartProcessSynchronizationService) {
        this.cartService = cartService;
        this.cartProcessSynchronizationService = cartProcessSynchronizationService;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long customerId = requiredLong(execution, "customerId");
        String action = requiredString(execution, "cartAction").toUpperCase();

        switch (action) {
            case "ADD" -> addItem(execution, customerId);
            case "REMOVE" -> removeItem(execution, customerId);
            case "CHECKOUT" -> checkout(execution, customerId);
            default -> throw new BadRequestException("Unsupported cart action: " + action);
        }
    }

    private void addItem(DelegateExecution execution, Long customerId) {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setShopItemId(requiredLong(execution, "shopItemId"));
        request.setQuantity(requiredPositiveInteger(execution, "quantity"));
        refreshCart(execution, customerId, cartService.addItem(customerId, request));
    }

    private void removeItem(DelegateExecution execution, Long customerId) {
        refreshCart(execution, customerId,
                cartService.removeItemIfPresent(customerId, requiredLong(execution, "removeShopItemId")));
    }

    private void checkout(DelegateExecution execution, Long customerId) {
        CartResponse cart = cartService.getCart(customerId);
        if (cart.getItems().isEmpty()) {
            refreshCart(execution, customerId, cart);
            return;
        }
        Object address = execution.getVariable("deliveryAddress");
        if (!(address instanceof String value) || value.isBlank()) {
            throw new BadRequestException("Delivery address is required for checkout");
        }
        execution.setVariable("checkoutRequested", true);
    }

    private void refreshCart(DelegateExecution execution, Long customerId, CartResponse cart) {
        String cartSummary = InitializeShoppingDelegate.cartSummary(cart);
        execution.setVariable("cartSummary", cartSummary);
        cartProcessSynchronizationService.synchronize(
                customerId,
                cartSummary,
                execution.getProcessInstanceId());
        execution.setVariable("checkoutRequested", false);
        execution.removeVariable("cartAction");
        execution.removeVariable("shopItemId");
        execution.removeVariable("removeShopItemId");
        execution.removeVariable("quantity");
    }

    private Long requiredLong(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (!(value instanceof Number number)) {
            throw new BadRequestException(name + " is required");
        }
        return number.longValue();
    }

    private int requiredPositiveInteger(DelegateExecution execution, String name) {
        Long value = requiredLong(execution, name);
        if (value <= 0 || value > Integer.MAX_VALUE) {
            throw new BadRequestException(name + " must be a positive integer");
        }
        return value.intValue();
    }

    private String requiredString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new BadRequestException(name + " is required");
        }
        return string.trim();
    }
}
