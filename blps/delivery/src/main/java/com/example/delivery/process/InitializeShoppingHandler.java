package com.example.delivery.process;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.dto.CartItemResponse;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.dto.ShopItemResponse;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.service.CartService;
import com.example.delivery.service.ShopItemService;
import com.example.delivery.service.UserService;

@Component
public class InitializeShoppingHandler implements DeliveryExternalTaskHandler {

    static final String EMPTY_CART_SUMMARY = "Cart is empty";

    private final UserService userService;
    private final ShopItemService shopItemService;
    private final CartService cartService;

    public InitializeShoppingHandler(
            UserService userService,
            ShopItemService shopItemService,
            CartService cartService) {
        this.userService = userService;
        this.shopItemService = shopItemService;
        this.cartService = cartService;
    }

    @Override
    public String topic() {
        return "delivery-initialize-shopping";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        User customer = resolveCustomer(task);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("customerId", customer.getId());
        variables.put("customerUsername", customer.getUsername());
        variables.put("catalogSummary", catalogSummary());
        variables.put("cartSummary", cartSummary(cartService.getCart(customer.getId())));
        variables.put("checkoutRequested", false);
        if (task.getVariable("directCheckout") == null) {
            variables.put("directCheckout", false);
        }
        return variables;
    }

    private User resolveCustomer(ExternalTask task) {
        Object customerId = task.getVariable("customerId");
        if (customerId instanceof Number number) {
            return userService.getUserWithRole(number.longValue(), UserRole.CUSTOMER);
        }
        return userService.getUserByUsernameWithRole(
                task.getVariable("customerUsername"), UserRole.CUSTOMER);
    }

    private String catalogSummary() {
        return shopItemService.listActiveItems().stream()
                .map(this::catalogLine)
                .collect(Collectors.joining("\n"));
    }

    private String catalogLine(ShopItemResponse item) {
        return "Product ID " + item.getId() + ": " + item.getName() + " - " + item.getPrice();
    }

    static String cartSummary(CartResponse cart) {
        if (cart.getItems().isEmpty()) {
            return EMPTY_CART_SUMMARY;
        }
        return cart.getItems().stream()
                .map(InitializeShoppingHandler::cartLine)
                .collect(Collectors.joining("\n"));
    }

    private static String cartLine(CartItemResponse item) {
        return "Product ID " + item.getShopItemId() + ": " + item.getProductName()
                + " x " + item.getQuantity() + " - " + item.getUnitPrice() + " each";
    }
}
