package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.OrderDecisionRequest;
import com.example.delivery.exception.ConflictException;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.messaging.MessageProducerService;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.model.User;
import com.example.delivery.repository.OrderRepository;

@Service
public class ShopService {

	private static final Logger log = LoggerFactory.getLogger(ShopService.class);

	private final OrderRepository orderRepository;
	private final UserService userService;
	private final NotificationService notificationService;
	private final MessageProducerService messageProducerService;
	private final Clock clock;

	public ShopService(
			OrderRepository orderRepository,
			UserService userService,
			NotificationService notificationService,
			MessageProducerService messageProducerService,
			Clock clock
	) {
		this.orderRepository = orderRepository;
		this.userService = userService;
		this.notificationService = notificationService;
		this.messageProducerService = messageProducerService;
		this.clock = clock;
	}

	@Transactional
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public Order acceptOrder(Long orderId) {
		User shop = userService.getDefaultShop();
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
		validateShopOrder(order, shop);
		if (order.getStatus() != OrderStatus.PENDING_SHOP) {
			throw new ConflictException("Order " + orderId + " is not waiting for shop decision");
		}
		OffsetDateTime now = OffsetDateTime.now(clock);
		order.setStatus(OrderStatus.ACCEPTED_WAITING_COURIER);
		order.setShopRespondedAt(now);
		order.setAcceptedAt(now);
		Order saved = orderRepository.save(order);
		messageProducerService.sendBitrixOrderSync(saved);

		
		notificationService.createNotification(
				order.getCustomer(),
				saved,
				NotificationType.SHOP_ACCEPTED,
				"Shop accepted order " + saved.getId()
		);

		
		messageProducerService.sendCourierAssignmentRequest(saved);

		log.info("Order {} accepted by shop. Courier assignment scheduled asynchronously",
				orderId);

		return saved;
	}

	@Transactional
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_ORDER_STATUS')")
	public Order declineOrder(Long orderId, OrderDecisionRequest request) {
		User shop = userService.getDefaultShop();
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
		validateShopOrder(order, shop);
		if (order.getStatus() != OrderStatus.PENDING_SHOP) {
			throw new ConflictException("Order " + orderId + " is not waiting for shop decision");
		}
		OffsetDateTime now = OffsetDateTime.now(clock);
		order.setStatus(OrderStatus.DECLINED_BY_SHOP);
		order.setShopRespondedAt(now);
		order.setCancelledAt(now);
		Order saved = orderRepository.save(order);
		messageProducerService.sendBitrixOrderSync(saved);

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
