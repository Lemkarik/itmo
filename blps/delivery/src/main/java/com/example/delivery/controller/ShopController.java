package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.OrderDecisionRequest;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.service.OrderService;
import com.example.delivery.service.ShopService;

@RestController
@RequestMapping("/api/shop/orders")
public class ShopController {

	private final ShopService shopService;
	private final OrderService orderService;

	public ShopController(ShopService shopService, OrderService orderService) {
		this.shopService = shopService;
		this.orderService = orderService;
	}

	@PostMapping("/{orderId}/accept")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public OrderResponse acceptOrder(@PathVariable Long orderId) {
		shopService.acceptOrder(orderId);
		return orderService.getOrder(orderId);
	}

	@PostMapping("/{orderId}/decline")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public OrderResponse declineOrder(
			@PathVariable Long orderId,
			@RequestBody(required = false) OrderDecisionRequest request
	) {
		shopService.declineOrder(orderId, request);
		return orderService.getOrder(orderId);
	}
}
