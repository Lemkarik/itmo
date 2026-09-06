package com.example.delivery.process;

import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.CartItemResponse;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.dto.ShopItemResponse;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.service.CartService;
import com.example.delivery.service.ShopItemService;
import com.example.delivery.service.UserService;

@Component("initializeShoppingDelegate")
public class InitializeShoppingDelegate implements JavaDelegate {

    static final String EMPTY_CART_SUMMARY = "Cart is empty";

    private final UserService userService;
    private final ShopItemService shopItemService;
    private final CartService cartService;

    public InitializeShoppingDelegate(
            UserService userService,
            ShopItemService shopItemService,
            CartService cartService) {
        this.userService = userService;
        this.shopItemService = shopItemService;
        this.cartService = cartService;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        User customer = resolveCustomer(execution);
        execution.setVariable("customerId", customer.getId());
        execution.setVariable("customerUsername", customer.getUsername());
        execution.setVariable("catalogSummary", catalogSummary());
        execution.setVariable("cartSummary", cartSummary(cartService.getCart(customer.getId())));
        execution.setVariable("checkoutRequested", false);
        if (!execution.hasVariable("directCheckout")) {
            execution.setVariable("directCheckout", false);
        }
    }

    private User resolveCustomer(DelegateExecution execution) {
        Object customerId = execution.getVariable("customerId");
        if (customerId instanceof Number number) {
            return userService.getUserWithRole(number.longValue(), UserRole.CUSTOMER);
        }
        return userService.getUserByUsernameWithRole(
                (String) execution.getVariable("customerUsername"),
                UserRole.CUSTOMER);
    }

    private String catalogSummary() {
        return shopItemService.listActiveItems().stream()
                .map(this::catalogLine)
                .collect(Collectors.joining("\n"));
    }

    private String catalogLine(ShopItemResponse item) {
        return "Product ID " + item.getId() + ": " + item.getName() + " — " + item.getPrice();
    }

    static String cartSummary(CartResponse cart) {
        if (cart.getItems().isEmpty()) {
            return EMPTY_CART_SUMMARY;
        }
        return cart.getItems().stream()
                .map(InitializeShoppingDelegate::cartLine)
                .collect(Collectors.joining("\n"));
    }

    private static String cartLine(CartItemResponse item) {
        return "Product ID " + item.getShopItemId() + ": " + item.getProductName()
                + " x " + item.getQuantity() + " — " + item.getUnitPrice() + " each";
    }
}
