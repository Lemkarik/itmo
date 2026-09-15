package com.example.delivery.process;

import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

import com.example.delivery.dto.UpdateCourierAvailabilityRequest;
import com.example.delivery.dto.UserResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.service.UserService;

@Component
public class UpdateCourierAvailabilityHandler implements DeliveryExternalTaskHandler {

    private final UserService userService;

    public UpdateCourierAvailabilityHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String topic() {
        return "delivery-update-courier-availability";
    }

    @Override
    public Map<String, Object> execute(ExternalTask task) {
        User courier = userService.getUserByUsernameWithRole(
                task.getVariable("courierUsername"), UserRole.COURIER);
        Object available = task.getVariable("available");
        if (!(available instanceof Boolean value)) {
            throw new BadRequestException("Availability is required");
        }
        UpdateCourierAvailabilityRequest request = new UpdateCourierAvailabilityRequest();
        request.setAvailable(value);
        UserResponse updated = userService.updateCourierAvailability(courier.getId(), request);
        return Map.of("availabilitySummary",
                "Courier " + courier.getUsername() + " is now "
                        + (Boolean.TRUE.equals(updated.getAvailable()) ? "available" : "unavailable"));
    }
}
