package com.example.delivery.process;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.exception.ConflictException;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.service.OrderService;

@Service
public class DeliveryProcessService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryProcessService.class);
    private static final String MANAGE_CART_TASK = "manageCart";

    public static final String PROCESS_KEY = "delivery-order";
    public static final String COURIER_AVAILABILITY_PROCESS_KEY = "courier-availability";
    public static final String ORDER_ID = "orderId";

    private final CamundaRestClient camundaRestClient;
    private final OrderService orderService;
    private final Duration orderStartTimeout;

    public DeliveryProcessService(
            CamundaRestClient camundaRestClient,
            OrderService orderService,
            @Value("${app.camunda.order-start-timeout-ms:15000}") long orderStartTimeoutMs) {
        this.camundaRestClient = camundaRestClient;
        this.orderService = orderService;
        this.orderStartTimeout = Duration.ofMillis(orderStartTimeoutMs);
    }

    public OrderResponse startOrderProcess(Long customerId, CreateOrderRequest request) {
        orderService.validateOrderCreation(customerId, request);
        List<CamundaRestClient.TaskReference> shoppingTasks = camundaRestClient.findTasks(
                PROCESS_KEY, MANAGE_CART_TASK, "customerId_eq_" + customerId);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("deliveryAddress", request.getDeliveryAddress());
        variables.put("requestedDeliveryTime",
                request.getDeliveryTime() == null ? null : request.getDeliveryTime().toString());

        String processInstanceId;
        if (shoppingTasks.isEmpty()) {
            variables.put("customerId", customerId);
            variables.put("directCheckout", true);
            processInstanceId = camundaRestClient.startProcess(PROCESS_KEY, variables);
        } else {
            CamundaRestClient.TaskReference shoppingTask = shoppingTasks.get(0);
            variables.put("cartAction", "CHECKOUT");
            camundaRestClient.completeTask(shoppingTask.id(), variables);
            processInstanceId = shoppingTask.processInstanceId();
        }

        OrderResponse order = awaitOrder(processInstanceId);
        closeOtherShoppingProcesses(shoppingTasks, processInstanceId);
        return order;
    }

    public void completeShopDecision(Long orderId, boolean accepted, String reason) {
        Order order = orderService.getOrderEntity(orderId);
        if (order.getStatus() != OrderStatus.PENDING_SHOP
                || order.getProcessInstanceId() == null
                || order.getProcessInstanceId().isBlank()) {
            throw new ConflictException("Order " + orderId + " is not waiting for a shop decision");
        }
        List<CamundaRestClient.TaskReference> tasks = awaitShopDecisionTask(order.getProcessInstanceId());
        if (tasks.size() != 1) {
            throw new ConflictException("Order " + orderId + " is not waiting for a shop decision");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("accepted", accepted);
        variables.put("declineReason", reason == null ? "" : reason.trim());
        camundaRestClient.completeTask(tasks.get(0).id(), variables);
    }

    public void correlateCourierAssigned(Order order) {
        if (order.getProcessInstanceId() == null || order.getProcessInstanceId().isBlank()) {
            return;
        }
        camundaRestClient.correlateMessage(
                "courierAssigned",
                order.getProcessInstanceId(),
                Map.of("courierId", order.getCourier().getId()));
    }

    private OrderResponse awaitOrder(String processInstanceId) {
        long deadline = System.nanoTime() + orderStartTimeout.toNanos();
        do {
            OrderResponse order = orderService.findByProcessInstanceId(processInstanceId);
            if (order != null) {
                return order;
            }
            pause();
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Camunda process did not create the order within "
                        + orderStartTimeout.toSeconds() + " seconds");
    }

    private List<CamundaRestClient.TaskReference> awaitShopDecisionTask(String processInstanceId) {
        long deadline = System.nanoTime() + orderStartTimeout.toNanos();
        do {
            List<CamundaRestClient.TaskReference> tasks = camundaRestClient.findTasksByProcessInstance(
                    PROCESS_KEY, "shopDecision", processInstanceId);
            if (!tasks.isEmpty()) {
                return tasks;
            }
            pause();
        } while (System.nanoTime() < deadline);
        return List.of();
    }

    private void closeOtherShoppingProcesses(
            List<CamundaRestClient.TaskReference> shoppingTasks,
            String completedProcessInstanceId) {
        shoppingTasks.stream()
                .map(CamundaRestClient.TaskReference::processInstanceId)
                .filter(processInstanceId -> !processInstanceId.equals(completedProcessInstanceId))
                .distinct()
                .forEach(processInstanceId -> {
                    try {
                        camundaRestClient.deleteProcessInstance(processInstanceId);
                    } catch (RuntimeException exception) {
                        log.warn("Could not close obsolete shopping process {}", processInstanceId, exception);
                    }
                });
    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Camunda", exception);
        }
    }
}
