package com.example.delivery.process;

import org.camunda.bpm.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartProcessSynchronizationService {

    private static final String MANAGE_CART_TASK = "manageCart";
    private static final String CUSTOMER_ID = "customerId";
    private static final String CART_SUMMARY = "cartSummary";

    private final TaskService taskService;

    public CartProcessSynchronizationService(TaskService taskService) {
        this.taskService = taskService;
    }

    @Transactional
    public void synchronize(Long customerId, String cartSummary, String currentProcessInstanceId) {
        taskService.createTaskQuery()
                .processDefinitionKey(DeliveryProcessService.PROCESS_KEY)
                .taskDefinitionKey(MANAGE_CART_TASK)
                .processVariableValueEquals(CUSTOMER_ID, customerId)
                .active()
                .list()
                .stream()
                .filter(task -> !task.getProcessInstanceId().equals(currentProcessInstanceId))
                .forEach(task -> taskService.setVariable(task.getId(), CART_SUMMARY, cartSummary));
    }
}
