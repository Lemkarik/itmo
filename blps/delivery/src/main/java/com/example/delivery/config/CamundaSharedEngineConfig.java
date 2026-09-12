package com.example.delivery.config;

import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.ProcessEngineService;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.spring.application.SpringProcessApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!worker")
public class CamundaSharedEngineConfig {

    @Bean
    public ProcessEngineService processEngineService() {
        return BpmPlatform.getProcessEngineService();
    }

    @Bean(destroyMethod = "")
    public ProcessEngine processEngine(ProcessEngineService processEngineService) {
        ProcessEngine processEngine = processEngineService.getDefaultProcessEngine();
        if (processEngine == null) {
            throw new IllegalStateException(
                    "Camunda default process engine is not available in WildFly");
        }
        return processEngine;
    }

    @Bean
    public SpringProcessApplication processApplication() {
        return new SpringProcessApplication();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }

    @Bean
    public AuthorizationService authorizationService(ProcessEngine processEngine) {
        return processEngine.getAuthorizationService();
    }
}
