package com.example.delivery.process;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.delivery.config.JwtUserDetails;
import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.service.OrderService;
import com.example.delivery.service.UserService;

@Component("createOrderDelegate")
public class CreateOrderDelegate implements JavaDelegate {

    private final OrderService orderService;
    private final UserService userService;
    private final CartProcessSynchronizationService cartProcessSynchronizationService;

    public CreateOrderDelegate(
            OrderService orderService,
            UserService userService,
            CartProcessSynchronizationService cartProcessSynchronizationService) {
        this.orderService = orderService;
        this.userService = userService;
        this.cartProcessSynchronizationService = cartProcessSynchronizationService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long customerId = numberVariable(execution, "customerId");
        if (customerId == null && SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof JwtUserDetails user) {
            customerId = user.getUserId();
        }
        if (customerId == null) {
            throw new BadRequestException("Customer identity is required to start an order process");
        }
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDeliveryAddress((String) execution.getVariable("deliveryAddress"));
        request.setDeliveryTime(toOffsetDateTime(execution.getVariable("requestedDeliveryTime")));
        OrderResponse order = orderService.createOrder(customerId, request);
        orderService.linkProcessInstance(order.getId(), execution.getProcessInstanceId());
        execution.setVariable(DeliveryProcessService.ORDER_ID, order.getId());
        execution.setVariable("shopUsername", userService.getDefaultShop().getUsername());
        execution.setVariable("cartSummary", InitializeShoppingDelegate.EMPTY_CART_SUMMARY);
        cartProcessSynchronizationService.synchronize(
                customerId,
                InitializeShoppingDelegate.EMPTY_CART_SUMMARY,
                execution.getProcessInstanceId());
        execution.setVariable("orderNumber", order.getId().toString());
        execution.setVariable("orderSummary", orderSummary(order));
    }

    private Long numberVariable(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value instanceof Number number ? number.longValue() : null;
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Date date) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof String string && !string.isBlank()) {
            String dateTime = string.trim();
            try {
                return OffsetDateTime.parse(dateTime);
            } catch (DateTimeParseException ignored) {
                try {
                    return LocalDateTime.parse(dateTime).atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException ignoredLocalDateTime) {
                    try {
                        return LocalDate.parse(dateTime)
                                .plusDays(1)
                                .atStartOfDay()
                                .minusNanos(1)
                                .atOffset(ZoneOffset.UTC);
                    } catch (DateTimeParseException ignoredLocalDate) {
                        throw new BadRequestException(
                                "Requested delivery time must be an ISO date or date-time");
                    }
                }
            }
        }
        return null;
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
