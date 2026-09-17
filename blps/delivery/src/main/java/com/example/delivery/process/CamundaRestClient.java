package com.example.delivery.process;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import com.example.delivery.model.UserRole;

@Component
public class CamundaRestClient {

    private final RestClient client;

    public CamundaRestClient(
            RestClient.Builder builder,
            @Value("${app.camunda.rest-url}") String restUrl,
            @Value("${app.camunda.username}") String username,
            @Value("${app.camunda.password}") String password) {
        this.client = builder
                .baseUrl(restUrl.replaceAll("/+$", ""))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeaders(headers -> headers.setBasicAuth(
                        username, password, StandardCharsets.UTF_8))
                .build();
    }

    public String startProcess(String processDefinitionKey, Map<String, Object> variables) {
        Map<String, Object> response = client.post()
                .uri("/process-definition/key/{key}/start", processDefinitionKey)
                .body(Map.of("variables", variableMap(variables)))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        Object processInstanceId = response == null ? null : response.get("id");
        if (processInstanceId == null || processInstanceId.toString().isBlank()) {
            throw new IllegalStateException("Camunda did not return a process instance id");
        }
        return processInstanceId.toString();
    }

    public List<TaskReference> findTasks(
            String processDefinitionKey,
            String taskDefinitionKey,
            String processVariableFilter) {
        List<Map<String, Object>> tasks = client.get()
                .uri(builder -> {
                    var uri = builder.path("/task")
                            .queryParam("processDefinitionKey", processDefinitionKey)
                            .queryParam("taskDefinitionKey", taskDefinitionKey)
                            .queryParam("active", true);
                    if (processVariableFilter != null && !processVariableFilter.isBlank()) {
                        uri.queryParam("processVariables", processVariableFilter);
                    }
                    return uri.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
        return taskReferences(tasks);
    }

    public List<TaskReference> findTasksByProcessInstance(
            String processDefinitionKey,
            String taskDefinitionKey,
            String processInstanceId) {
        List<Map<String, Object>> tasks = client.get()
                .uri(builder -> builder.path("/task")
                        .queryParam("processDefinitionKey", processDefinitionKey)
                        .queryParam("taskDefinitionKey", taskDefinitionKey)
                        .queryParam("processInstanceId", processInstanceId)
                        .queryParam("active", true)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
        return taskReferences(tasks);
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        client.post()
                .uri("/task/{id}/complete", taskId)
                .body(Map.of("variables", variableMap(variables)))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteProcessInstance(String processInstanceId) {
        try {
            client.delete()
                    .uri("/process-instance/{id}", processInstanceId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
        }
    }

    public void setProcessVariable(String processInstanceId, String name, Object value) {
        client.put()
                .uri("/process-instance/{id}/variables/{name}", processInstanceId, name)
                .body(variable(value))
                .retrieve()
                .toBodilessEntity();
    }

    public void correlateMessage(
            String messageName,
            String processInstanceId,
            Map<String, Object> variables) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("messageName", messageName);
        request.put("processInstanceId", processInstanceId);
        request.put("processVariables", variableMap(variables));
        request.put("resultEnabled", false);
        client.post()
                .uri("/message")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public boolean userExists(String username) {
        try {
            client.get()
                    .uri("/user/{id}/profile", username)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return false;
            }
            throw exception;
        }
    }

    public void createUser(String username, String name, UserRole role) {
        Map<String, Object> request = Map.of(
                "profile", Map.of(
                        "id", username,
                        "firstName", name,
                        "lastName", role.name()),
                "credentials", Map.of("password", UUID.randomUUID().toString()));
        client.post()
                .uri("/user/create")
                .body(request)
                .retrieve()
                .toBodilessEntity();
        try {
            client.put()
                    .uri("/group/{group}/members/{id}", role.name(), username)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            deleteUser(username);
            throw exception;
        }
    }

    public void deleteUser(String username) {
        try {
            client.delete()
                    .uri("/user/{id}", username)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
        }
    }

    private Map<String, VariableValue> variableMap(Map<String, Object> variables) {
        Map<String, VariableValue> result = new LinkedHashMap<>();
        variables.forEach((name, value) -> {
            if (value != null) {
                result.put(name, variable(value));
            }
        });
        return result;
    }

    private VariableValue variable(Object value) {
        if (value instanceof Long) {
            return new VariableValue(value, "Long");
        }
        if (value instanceof Integer) {
            return new VariableValue(value, "Integer");
        }
        if (value instanceof Boolean) {
            return new VariableValue(value, "Boolean");
        }
        return new VariableValue(value.toString(), "String");
    }

    private List<TaskReference> taskReferences(List<Map<String, Object>> tasks) {
        if (tasks == null) {
            return List.of();
        }
        return tasks.stream()
                .map(task -> new TaskReference(
                        requiredResponseValue(task, "id"),
                        requiredResponseValue(task, "processInstanceId")))
                .toList();
    }

    private String requiredResponseValue(Map<String, Object> response, String name) {
        Object value = response.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("Camunda response does not contain " + name);
        }
        return value.toString();
    }

    private record VariableValue(Object value, String type) {
    }

    public record TaskReference(String id, String processInstanceId) {
    }
}
