package com.example.delivery.service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.delivery.jca.Bitrix24EisService;
import com.example.delivery.jca.Bitrix24IntegrationException;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderItem;
import com.example.delivery.model.OrderStatus;

@Service
public class BitrixOrderSyncService {

    private final Bitrix24EisService bitrix24EisService;
    private final String pendingShopStage;
    private final String acceptedStage;
    private final String courierAssignedStage;
    private final String declinedStage;
    private final String noCourierStage;

    public BitrixOrderSyncService(
            Bitrix24EisService bitrix24EisService,
            @Value("${app.eis.bitrix24.stage.pending-shop:C1:NEW}") String pendingShopStage,
            @Value("${app.eis.bitrix24.stage.accepted-waiting-courier:C1:PREPARATION}") String acceptedStage,
            @Value("${app.eis.bitrix24.stage.courier-assigned:C1:WON}") String courierAssignedStage,
            @Value("${app.eis.bitrix24.stage.declined-by-shop:C1:LOSE}") String declinedStage,
            @Value("${app.eis.bitrix24.stage.cancelled-no-courier:C1:APOLOGY}") String noCourierStage) {
        this.bitrix24EisService = bitrix24EisService;
        this.pendingShopStage = pendingShopStage;
        this.acceptedStage = acceptedStage;
        this.courierAssignedStage = courierAssignedStage;
        this.declinedStage = declinedStage;
        this.noCourierStage = noCourierStage;
    }

    public void synchronize(Order order) throws Bitrix24IntegrationException {
        String stageId = stageId(order.getStatus());
        if (order.getBitrixDealId() == null) {
            Long crmItemId = bitrix24EisService.createOrder(
                    order.getId(),
                    "Delivery Order #" + order.getId(),
                    stageId,
                    order.getCustomer().getName(),
                    order.getDeliveryAddress(),
                    order.getDeliveryTime(),
                    totalAmount(order),
                    buildComments(order));
            order.setBitrixDealId(crmItemId);
            return;
        }
        bitrix24EisService.updateOrderStatus(order.getBitrixDealId(), stageId);
    }

    private String stageId(OrderStatus status) {
        return switch (status) {
            case PENDING_SHOP -> pendingShopStage;
            case ACCEPTED_WAITING_COURIER -> acceptedStage;
            case COURIER_ASSIGNED -> courierAssignedStage;
            case DECLINED_BY_SHOP -> declinedStage;
            case CANCELLED_NO_COURIER -> noCourierStage;
        };
    }

    private BigDecimal totalAmount(Order order) {
        return order.getItems().stream()
                .map(this::itemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal itemAmount(OrderItem item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private String buildComments(Order order) {
        String items = order.getItems().stream()
                .map(item -> item.getProductName() + " x " + item.getQuantity())
                .collect(Collectors.joining(", "));
        return "Local order ID: " + order.getId()
                + "\nCustomer: " + order.getCustomer().getName()
                + "\nDelivery address: " + order.getDeliveryAddress()
                + "\nDelivery time: " + order.getDeliveryTime()
                + "\nItems: " + items;
    }
}
