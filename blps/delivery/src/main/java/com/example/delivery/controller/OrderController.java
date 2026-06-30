package com.example.delivery.controller;

import com.example.delivery.config.JwtUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.ListResponse;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.service.OrderService;

@RestController
@RequestMapping("/api")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/orders")
	@PreAuthorize("hasAuthority('PRIVILEGE_CREATE_ORDER')")
	public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
		Long customerId = getCurrentUserId();
		return orderService.createOrder(customerId, request);
	}

	@GetMapping("/orders/{orderId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_OWN_ORDERS') or hasAuthority('PRIVILEGE_VIEW_ALL_ORDERS')")
	public OrderResponse getOrder(@PathVariable Long orderId) {
		return orderService.getOrder(orderId);
	}

	@GetMapping("/my/orders")
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_OWN_ORDERS')")
	public ListResponse<OrderResponse> listMyOrders(
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		Long customerId = getCurrentUserId();
		return new ListResponse<>(orderService.listOrdersByCustomer(customerId, new PaginationRequest(page, pageSize)));
	}

	@GetMapping("/shop/orders")
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_ALL_ORDERS')")
	public ListResponse<OrderResponse> listShopOrders(
			@RequestParam(name = "status", required = false) OrderStatus status,
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		return new ListResponse<>(orderService.listOrdersByShop(status, new PaginationRequest(page, pageSize)));
	}

	@GetMapping("/my/courier/orders")
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_OWN_ORDERS')")
	public ListResponse<OrderResponse> listMyCourierOrders(
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		Long courierId = getCurrentUserId();
		return new ListResponse<>(orderService.listOrdersByCourier(courierId, new PaginationRequest(page, pageSize)));
	}

	private Long getCurrentUserId() {
		return ((JwtUserDetails) org.springframework.security.core.context.SecurityContextHolder
				.getContext().getAuthentication().getPrincipal()).getUserId();
	}
}
