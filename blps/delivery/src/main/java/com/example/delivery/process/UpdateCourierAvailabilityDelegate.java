package com.example.delivery.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.UpdateCourierAvailabilityRequest;
import com.example.delivery.dto.UserResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.service.UserService;

@Component("updateCourierAvailabilityDelegate")
public class UpdateCourierAvailabilityDelegate implements JavaDelegate {

    private final UserService userService;

    public UpdateCourierAvailabilityDelegate(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        User courier = userService.getUserByUsernameWithRole(
                (String) execution.getVariable("courierUsername"),
                UserRole.COURIER);
        Object available = execution.getVariable("available");
        if (!(available instanceof Boolean value)) {
            throw new BadRequestException("Availability is required");
        }
        UpdateCourierAvailabilityRequest request = new UpdateCourierAvailabilityRequest();
        request.setAvailable(value);
        UserResponse updated = userService.updateCourierAvailability(courier.getId(), request);
        execution.setVariable("availabilitySummary",
                "Courier " + courier.getUsername() + " is now "
                        + (Boolean.TRUE.equals(updated.getAvailable()) ? "available" : "unavailable"));
    }
}
