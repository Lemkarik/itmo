package com.example.delivery.process;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.exception.ConflictException;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.model.Order;
import com.example.delivery.service.OrderService;

@Service
public class DeliveryProcessService {

    public static final String PROCESS_KEY = "delivery-order";
    public static final String COURIER_AVAILABILITY_PROCESS_KEY = "courier-availability";
    public static final String ORDER_ID = "orderId";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final OrderService orderService;

    public DeliveryProcessService(RuntimeService runtimeService, TaskService taskService, OrderService orderService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.orderService = orderService;
    }

    @Transactional
    public OrderResponse startOrderProcess(Long customerId, CreateOrderRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", customerId);
        variables.put("deliveryAddress", request.getDeliveryAddress());
        variables.put("requestedDeliveryTime", request.getDeliveryTime());
        variables.put("directCheckout", true);
        ProcessInstance process = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
        Number orderId = (Number) runtimeService.getVariable(process.getId(), ORDER_ID);
        if (orderId == null) {
            throw new IllegalStateException("The order process did not create an order");
        }
        return orderService.getOrder(orderId.longValue());
    }

    @Transactional
    public void completeShopDecision(Long orderId, boolean accepted, String reason) {
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .taskDefinitionKey("shopDecision")
                .processVariableValueEquals(ORDER_ID, orderId)
                .singleResult();
        if (task == null) {
            throw new ConflictException("Order " + orderId + " is not waiting for a shop decision");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("accepted", accepted);
        variables.put("declineReason", reason == null ? "" : reason.trim());
        taskService.complete(task.getId(), variables);
    }

    @Transactional
    public void correlateCourierAssigned(Order order) {
        if (order.getProcessInstanceId() == null || order.getProcessInstanceId().isBlank()) {
            return;
        }
        runtimeService.createMessageCorrelation("courierAssigned")
                .processInstanceId(order.getProcessInstanceId())
                .setVariable("courierId", order.getCourier().getId())
                .correlate();
    }
}
