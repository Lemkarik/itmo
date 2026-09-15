package com.example.delivery.process;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.ProcessedExternalTask;
import com.example.delivery.repository.ProcessedExternalTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExternalTaskExecutionService {

    private static final TypeReference<Map<String, Object>> RESULT_TYPE = new TypeReference<>() { };

    private final ProcessedExternalTaskRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ExternalTaskExecutionService(
            ProcessedExternalTaskRepository repository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public Map<String, Object> execute(DeliveryExternalTaskHandler handler, ExternalTask task) {
        return repository.findById(task.getId())
                .map(processed -> read(processed.getResultJson()))
                .orElseGet(() -> executeOnce(handler, task));
    }

    private Map<String, Object> executeOnce(DeliveryExternalTaskHandler handler, ExternalTask task) {
        Map<String, Object> result = handler.execute(task);
        repository.save(new ProcessedExternalTask(
                task.getId(), write(result), OffsetDateTime.now(clock)));
        return result;
    }

    private String write(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize external task result", exception);
        }
    }

    private Map<String, Object> read(String result) {
        try {
            return objectMapper.readValue(result, RESULT_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize external task result", exception);
        }
    }
}
