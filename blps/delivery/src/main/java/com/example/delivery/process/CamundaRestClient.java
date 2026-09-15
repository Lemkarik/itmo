package com.example.delivery.process;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

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
        ProcessInstanceResponse response = client.post()
                .uri("/process-definition/key/{key}/start", processDefinitionKey)
                .body(Map.of("variables", variableMap(variables)))
                .retrieve()
                .body(ProcessInstanceResponse.class);
        if (response == null || response.id() == null) {
            throw new IllegalStateException("Camunda did not return a process instance id");
        }
        return response.id();
    }

    public Long getLongProcessVariable(String processInstanceId, String variableName) {
        VariableResponse response;
        try {
            response = client.get()
                    .uri("/process-instance/{id}/variables/{name}", processInstanceId, variableName)
                    .retrieve()
                    .body(VariableResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return null;
            }
            throw exception;
        }
        if (response == null || response.value() == null) {
            return null;
        }
        if (response.value() instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(response.value().toString());
    }

    public List<TaskReference> findTasks(
            String processDefinitionKey,
            String taskDefinitionKey,
            String processVariableFilter) {
        List<TaskReference> tasks = client.get()
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
                .body(new ParameterizedTypeReference<List<TaskReference>>() { });
        return tasks == null ? List.of() : tasks;
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        client.post()
                .uri("/task/{id}/complete", taskId)
                .body(Map.of("variables", variableMap(variables)))
                .retrieve()
                .toBodilessEntity();
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

    private record ProcessInstanceResponse(String id) {
    }

    private record VariableResponse(Object value, String type) {
    }

    private record VariableValue(Object value, String type) {
    }

    public record TaskReference(String id, String processInstanceId) {
    }
}
