package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.CreateOrderRequest;
import com.example.delivery.dto.OrderItemResponse;
import com.example.delivery.dto.OrderResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.model.Cart;
import com.example.delivery.model.CartItem;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderItem;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.OrderRepository;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final UserService userService;
	private final CartService cartService;
	private final NotificationService notificationService;
	private final Clock clock;

	public OrderService(
			OrderRepository orderRepository,
			UserService userService,
			CartService cartService,
			NotificationService notificationService,
			Clock clock
	) {
		this.orderRepository = orderRepository;
		this.userService = userService;
		this.cartService = cartService;
		this.notificationService = notificationService;
		this.clock = clock;
	}

	@Transactional
	public OrderResponse createOrder(Long customerId, CreateOrderRequest request) {
		if (request == null) {
			throw new BadRequestException("Request body is required");
		}
		if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
			throw new BadRequestException("Delivery address is required");
		}

		User customer = userService.getUserWithRole(customerId, UserRole.CUSTOMER);
		User shop = userService.getDefaultShop();

		Cart cart = cartService.getOrCreateCart(customer);
		if (cart.getItems().isEmpty()) {
			throw new BadRequestException("Cart is empty");
		}

		Order order = new Order();
		order.setCustomer(customer);
		order.setShop(shop);
		order.setStatus(OrderStatus.PENDING_SHOP);
		order.setDeliveryAddress(request.getDeliveryAddress().trim());
		order.setDeliveryTime(request.getDeliveryTime());
		OffsetDateTime now = OffsetDateTime.now(clock);
		order.setCreatedAt(now);
		order.setUpdatedAt(now);

		for (CartItem cartItem : cart.getItems()) {
			if (!cartItem.getShopItem().getShop().getId().equals(shop.getId())) {
				throw new BadRequestException("Cart contains items from an unexpected shop");
			}
			OrderItem orderItem = new OrderItem(
					order,
					cartItem.getShopItem(),
					cartItem.getProductName(),
					cartItem.getQuantity(),
					cartItem.getUnitPrice()
			);
			order.getItems().add(orderItem);
		}

		Order saved = orderRepository.save(order);
		cartService.clearCart(cart);

		notificationService.createNotification(
				shop,
				saved,
				NotificationType.ORDER_CREATED,
				"New order " + saved.getId() + " from customer " + customer.getName()
		);
		notificationService.createNotification(
				customer,
				saved,
				NotificationType.ORDER_CREATED,
				"Order " + saved.getId() + " created and sent to shop"
		);

		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
		return toResponse(order);
	}

	@Transactional(readOnly = true)
	public Order getOrderEntity(Long orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> listOrdersByCustomer(Long customerId, PaginationRequest pagination) {
		userService.getUserWithRole(customerId, UserRole.CUSTOMER);
		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pagination.toPageable())
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> listOrdersByShop(OrderStatus status, PaginationRequest pagination) {
		User shop = userService.getDefaultShop();
		return (status == null
				? orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId(), pagination.toPageable())
				: orderRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shop.getId(), status, pagination.toPageable()))
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> listOrdersByCourier(Long courierId, PaginationRequest pagination) {
		userService.getUserWithRole(courierId, UserRole.COURIER);
		return orderRepository.findByCourierIdOrderByCreatedAtDesc(courierId, pagination.toPageable())
				.map(this::toResponse);
	}

	public OrderResponse toResponse(Order order) {
		List<OrderItemResponse> items = order.getItems().stream()
				.map(item -> new OrderItemResponse(
						item.getId(),
						item.getShopItem().getId(),
						item.getProductName(),
						item.getQuantity(),
						item.getUnitPrice()
				))
				.toList();
		Long courierId = order.getCourier() != null ? order.getCourier().getId() : null;
		return new OrderResponse(
				order.getId(),
				order.getStatus(),
				order.getCustomer().getId(),
				order.getShop().getId(),
				courierId,
				order.getDeliveryAddress(),
				order.getDeliveryTime(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				order.getShopRespondedAt(),
				order.getAcceptedAt(),
				order.getCancelledAt(),
				order.getCourierAssignedAt(),
				items
		);
	}
}
