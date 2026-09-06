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
import com.example.delivery.process.DeliveryProcessService;

@RestController
@RequestMapping("/api/shop/orders")
public class ShopController {

	private final OrderService orderService;
	private final DeliveryProcessService deliveryProcessService;

	public ShopController(OrderService orderService, DeliveryProcessService deliveryProcessService) {
		this.orderService = orderService;
		this.deliveryProcessService = deliveryProcessService;
	}

	@PostMapping("/{orderId}/accept")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public OrderResponse acceptOrder(@PathVariable Long orderId) {
		deliveryProcessService.completeShopDecision(orderId, true, null);
		return orderService.getOrder(orderId);
	}

	@PostMapping("/{orderId}/decline")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public OrderResponse declineOrder(
			@PathVariable Long orderId,
			@RequestBody(required = false) OrderDecisionRequest request
	) {
		deliveryProcessService.completeShopDecision(orderId, false, request == null ? null : request.getReason());
		return orderService.getOrder(orderId);
	}
}
