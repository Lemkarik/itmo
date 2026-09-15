package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;

public interface DeliveryExternalTaskHandler {

    String topic();

    Map<String, Object> execute(ExternalTask task);
}
