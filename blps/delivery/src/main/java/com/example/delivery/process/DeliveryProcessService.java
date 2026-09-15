package com.example.delivery.process;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.exception.ConflictException;
import com.example.delivery.model.Order;
import com.example.delivery.service.OrderService;

@Service
public class DeliveryProcessService {

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
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("customerId", customerId);
        variables.put("deliveryAddress", request.getDeliveryAddress());
        variables.put("requestedDeliveryTime",
                request.getDeliveryTime() == null ? null : request.getDeliveryTime().toString());
        variables.put("directCheckout", true);
        String processInstanceId = camundaRestClient.startProcess(PROCESS_KEY, variables);
        Long orderId = awaitOrderId(processInstanceId);
        return orderService.getOrder(orderId);
    }

    public void completeShopDecision(Long orderId, boolean accepted, String reason) {
        List<CamundaRestClient.TaskReference> tasks = camundaRestClient.findTasks(
                PROCESS_KEY, "shopDecision", ORDER_ID + "_eq_" + orderId);
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

    private Long awaitOrderId(String processInstanceId) {
        long deadline = System.nanoTime() + orderStartTimeout.toNanos();
        do {
            Long orderId = camundaRestClient.getLongProcessVariable(processInstanceId, ORDER_ID);
            if (orderId != null) {
                return orderId;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for order creation", exception);
            }
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException(
                "Camunda process did not create the order within " + orderStartTimeout.toSeconds() + " seconds");
    }
}
