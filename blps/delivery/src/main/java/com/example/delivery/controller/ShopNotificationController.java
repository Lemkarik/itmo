package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.ListResponse;
import com.example.delivery.dto.NotificationResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.model.User;
import com.example.delivery.service.NotificationService;
import com.example.delivery.service.UserService;

@RestController
@RequestMapping("/api/shop/notifications")
public class ShopNotificationController {

	private final NotificationService notificationService;
	private final UserService userService;

	public ShopNotificationController(NotificationService notificationService, UserService userService) {
		this.notificationService = notificationService;
		this.userService = userService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_SHOP_NOTIFICATIONS')")
	public ListResponse<NotificationResponse> listShopNotifications(
			@RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		User shop = userService.getDefaultShop();
		return new ListResponse<>(
				notificationService.listNotifications(shop.getId(), unreadOnly, new PaginationRequest(page, pageSize))
		);
	}
}
