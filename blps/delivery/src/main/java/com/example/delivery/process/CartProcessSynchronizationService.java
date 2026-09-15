package com.example.delivery.process;

import org.springframework.stereotype.Service;

@Service
public class CartProcessSynchronizationService {

    private static final String MANAGE_CART_TASK = "manageCart";

    private final CamundaRestClient camundaRestClient;

    public CartProcessSynchronizationService(CamundaRestClient camundaRestClient) {
        this.camundaRestClient = camundaRestClient;
    }

    public void synchronize(Long customerId, String cartSummary, String currentProcessInstanceId) {
        camundaRestClient.findTasks(
                        DeliveryProcessService.PROCESS_KEY,
                        MANAGE_CART_TASK,
                        "customerId_eq_" + customerId)
                .stream()
                .filter(task -> !task.processInstanceId().equals(currentProcessInstanceId))
                .forEach(task -> camundaRestClient.setProcessVariable(
                        task.processInstanceId(), "cartSummary", cartSummary));
    }
}
