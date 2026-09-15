package com.example.delivery.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.interceptor.auth.BasicAuthProvider;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.example.delivery.process.DeliveryExternalTaskHandler;
import com.example.delivery.process.ExternalTaskExecutionService;

@Configuration
@Profile("!worker")
public class CamundaExternalTaskConfig {

    private static final Logger log = LoggerFactory.getLogger(CamundaExternalTaskConfig.class);

    @Bean(destroyMethod = "stop")
    public ExternalTaskClient externalTaskClient(
            @Value("${app.camunda.rest-url}") String restUrl,
            @Value("${app.camunda.username}") String username,
            @Value("${app.camunda.password}") String password,
            @Value("${app.camunda.external-tasks.worker-id}") String workerId,
            @Value("${app.camunda.external-tasks.lock-duration-ms:60000}") long lockDuration,
            @Value("${app.camunda.external-tasks.async-response-timeout-ms:20000}") long asyncResponseTimeout) {
        return ExternalTaskClient.create()
                .baseUrl(restUrl.replaceAll("/+$", ""))
                .workerId(workerId)
                .lockDuration(lockDuration)
                .asyncResponseTimeout(asyncResponseTimeout)
                .addInterceptor(new BasicAuthProvider(username, password))
                .build();
    }

    @Bean
    public ApplicationRunner externalTaskSubscriptions(
            ExternalTaskClient client,
            List<DeliveryExternalTaskHandler> handlers,
            ExternalTaskExecutionService executionService,
            @Value("${app.camunda.external-tasks.retry-timeout-ms:15000}") long retryTimeout) {
        return arguments -> handlers.forEach(handler -> client
                .subscribe(handler.topic())
                .handler((task, taskService) -> execute(
                        handler, task, taskService, executionService, retryTimeout))
                .open());
    }

    private void execute(
            DeliveryExternalTaskHandler handler,
            ExternalTask task,
            ExternalTaskService taskService,
            ExternalTaskExecutionService executionService,
            long retryTimeout) {
        try {
            taskService.complete(task, executionService.execute(handler, task));
        } catch (RuntimeException exception) {
            int retries = task.getRetries() == null ? 2 : Math.max(0, task.getRetries() - 1);
            StringWriter details = new StringWriter();
            exception.printStackTrace(new PrintWriter(details));
            log.error("External task {} on topic {} failed", task.getId(), handler.topic(), exception);
            taskService.handleFailure(
                    task,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                    details.toString(),
                    retries,
                    retryTimeout);
        }
    }
}
