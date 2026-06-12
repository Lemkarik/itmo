package com.example.delivery.service;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.CreateUserRequest;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.dto.UpdateCourierAvailabilityRequest;
import com.example.delivery.dto.UserResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.model.Privilege;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.UserRepository;
@Service
public class UserService {

	private final UserRepository userRepository;
	private final CourierAssignmentService courierAssignmentService;

	public UserService(UserRepository userRepository, CourierAssignmentService courierAssignmentService) {
		this.userRepository = userRepository;
		this.courierAssignmentService = courierAssignmentService;
	}
	@Transactional
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_USERS')")
	public UserResponse createUser(CreateUserRequest request) {
		if (request == null) {
			throw new BadRequestException("Request body is required");
		}
		if (request.getName() == null || request.getName().isBlank()) {
			throw new BadRequestException("Name is required");
		}
		if (request.getRole() == null) {
			throw new BadRequestException("Role is required");
		}
		if (request.getRole() == UserRole.SHOP) {
			throw new BadRequestException("Shop user is created by the system");
		}
		boolean available = request.getRole() == UserRole.COURIER && Boolean.TRUE.equals(request.getAvailable());
		User user = new User(request.getName().trim(), request.getRole(), available);
		User saved = userRepository.save(user);
		if (Boolean.TRUE.equals(saved.getAvailable())) {
			courierAssignmentService.assignNextWaitingOrderToCourier(saved);
		}
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> listUsers(UserRole role, PaginationRequest pagination) {
		return (role == null
				? userRepository.findAllByOrderByIdAsc(pagination.toPageable())
				: userRepository.findByRoleOrderByIdAsc(role, pagination.toPageable()))
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public User getUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("User not found: " + id));
	}

	@Transactional(readOnly = true)
	public User getUserWithRole(Long id, UserRole role) {
		User user = getUser(id);
		if (user.getRole() != role) {
			throw new BadRequestException("User " + id + " is not a " + role);
		}
		return user;
	}

	@Transactional(readOnly = true)
	public User getDefaultShop() {
		return userRepository.findFirstByRoleOrderByIdAsc(UserRole.SHOP)
				.orElseThrow(() -> new NotFoundException("Default shop is not initialized"));
	}
	@Transactional
	public UserResponse updateCourierAvailability(Long courierId, UpdateCourierAvailabilityRequest request) {
		if (request == null || request.getAvailable() == null) {
			throw new BadRequestException("Availability is required");
		}
		User courier = getUserWithRole(courierId, UserRole.COURIER);
		courier.setAvailable(request.getAvailable());
		User saved = userRepository.save(courier);
		if (Boolean.TRUE.equals(saved.getAvailable())) {
			courierAssignmentService.assignNextWaitingOrderToCourier(saved);
		}
		return toResponse(saved);
	}

	private UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getRole(), user.getAvailable());
	}
}

