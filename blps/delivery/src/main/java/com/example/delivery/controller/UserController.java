package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.CreateUserRequest;
import com.example.delivery.dto.ListResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.dto.UpdateCourierAvailabilityRequest;
import com.example.delivery.dto.UserResponse;
import com.example.delivery.model.UserRole;
import com.example.delivery.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/users")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_USERS')")
	public UserResponse createUser(@RequestBody CreateUserRequest request) {
		return userService.createUser(request);
	}

	@GetMapping("/users")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_USERS')")
	public ListResponse<UserResponse> listUsers(
			@RequestParam(name = "role", required = false) UserRole role,
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		return new ListResponse<>(userService.listUsers(role, new PaginationRequest(page, pageSize)));
	}

	@PatchMapping("/couriers/{courierId}/availability")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_COURIER_AVAILABILITY') and @authSecurity.isCurrentUser(#courierId)")
	public UserResponse updateCourierAvailability(
			@PathVariable Long courierId,
			@RequestBody UpdateCourierAvailabilityRequest request
	) {
		return userService.updateCourierAvailability(courierId, request);
	}
}
