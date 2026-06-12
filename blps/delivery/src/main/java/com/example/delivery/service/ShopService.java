package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.OrderDecisionRequest;
import com.example.delivery.exception.ConflictException;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.model.Privilege;
import com.example.delivery.model.User;
import com.example.delivery.repository.OrderRepository;

@Service
public class ShopService {

	private final OrderRepository orderRepository;
	private final UserService userService;
	private final NotificationService notificationService;
	private final CourierAssignmentService courierAssignmentService;
	private final Clock clock;

	public ShopService(
			OrderRepository orderRepository,
			UserService userService,
			NotificationService notificationService,
			CourierAssignmentService courierAssignmentService,
			Clock clock
	) {
		this.orderRepository = orderRepository;
		this.userService = userService;
		this.notificationService = notificationService;
		this.courierAssignmentService = courierAssignmentService;
		this.clock = clock;
	}

	@Transactional
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public Order acceptOrder(Long orderId) {
		User shop = userService.getDefaultShop();
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new com.example.delivery.exception.NotFoundException("Order not found: " + orderId));
		validateShopOrder(order, shop);
		if (order.getStatus() != OrderStatus.PENDING_SHOP) {
			throw new ConflictException("Order " + orderId + " is not waiting for shop decision");
		}
		OffsetDateTime now = OffsetDateTime.now(clock);
		order.setStatus(OrderStatus.ACCEPTED_WAITING_COURIER);
		order.setShopRespondedAt(now);
		order.setAcceptedAt(now);
		Order saved = orderRepository.save(order);

		notificationService.createNotification(
				order.getCustomer(),
				saved,
				NotificationType.SHOP_ACCEPTED,
				"Shop accepted order " + saved.getId()
		);

		courierAssignmentService.tryAssignCourier(saved);
		return saved;
	}

	@Transactional
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public Order declineOrder(Long orderId, OrderDecisionRequest request) {
		User shop = userService.getDefaultShop();
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new com.example.delivery.exception.NotFoundException("Order not found: " + orderId));
		validateShopOrder(order, shop);
		if (order.getStatus() != OrderStatus.PENDING_SHOP) {
			throw new ConflictException("Order " + orderId + " is not waiting for shop decision");
		}
		OffsetDateTime now = OffsetDateTime.now(clock);
		order.setStatus(OrderStatus.DECLINED_BY_SHOP);
		order.setShopRespondedAt(now);
		order.setCancelledAt(now);
		Order saved = orderRepository.save(order);

		String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
				? " Reason: " + request.getReason().trim()
				: "";

		notificationService.createNotification(
				order.getCustomer(),
				saved,
				NotificationType.SHOP_DECLINED,
				"Shop declined order " + saved.getId() + "." + reason
		);
		notificationService.createNotification(
				shop,
				saved,
				NotificationType.SHOP_DECLINED,
				"Order " + saved.getId() + " declined." + reason
		);

		return saved;
	}

	private void validateShopOrder(Order order, User shop) {
		if (!order.getShop().getId().equals(shop.getId())) {
			throw new ConflictException("Order " + order.getId() + " does not belong to shop " + shop.getId());
		}
	}
}
