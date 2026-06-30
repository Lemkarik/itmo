package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.config.JwtUserDetails;
import com.example.delivery.dto.ListResponse;
import com.example.delivery.dto.NotificationResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_NOTIFICATIONS')")
	public ListResponse<NotificationResponse> listNotifications(
			@RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		Long userId = getCurrentUserId();
		return new ListResponse<>(
				notificationService.listNotifications(userId, unreadOnly, new PaginationRequest(page, pageSize))
		);
	}

	@PostMapping("/{notificationId}/read")
	@PreAuthorize("hasAuthority('PRIVILEGE_MARK_NOTIFICATION_READ')")
	public NotificationResponse markRead(@PathVariable Long notificationId) {
		return notificationService.markRead(notificationId);
	}

	private Long getCurrentUserId() {
		return ((JwtUserDetails) SecurityContextHolder
				.getContext().getAuthentication().getPrincipal()).getUserId();
	}
}
