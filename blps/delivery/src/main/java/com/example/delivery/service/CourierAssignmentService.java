package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.jca.LogisticsException;
import com.example.delivery.jca.YandexMapsEisService;
import com.example.delivery.messaging.AsyncEventPublisher;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.repository.UserRepository;

@Service
public class CourierAssignmentService {

	private static final Logger log = LoggerFactory.getLogger(CourierAssignmentService.class);

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final AsyncEventPublisher asyncEventPublisher;
	private final Clock clock;
	private final YandexMapsEisService yandexMapsEisService;
	private final String shopAddress;

	public CourierAssignmentService(
			OrderRepository orderRepository,
			UserRepository userRepository,
			NotificationService notificationService,
			AsyncEventPublisher asyncEventPublisher,
			Clock clock,
			YandexMapsEisService yandexMapsEisService,
			@Value("${app.shop.address:}") String shopAddress
	) {
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.asyncEventPublisher = asyncEventPublisher;
		this.clock = clock;
		this.yandexMapsEisService = yandexMapsEisService;
		this.shopAddress = shopAddress;
	}

	@Transactional
	public boolean tryAssignCourier(Order order) {
		if (order.getStatus() != OrderStatus.ACCEPTED_WAITING_COURIER) {
			return false;
		}
		Optional<User> courierOpt = userRepository.findFirstByRoleAndAvailableTrueOrderByIdAsc(UserRole.COURIER);
		if (courierOpt.isEmpty()) {
			return false;
		}
		User courier = courierOpt.get();
		assignCourier(order, courier);
		return true;
	}

	@Transactional
	public boolean assignNextWaitingOrderToCourier(User courier) {
		if (courier.getRole() != UserRole.COURIER || !Boolean.TRUE.equals(courier.getAvailable())) {
			return false;
		}
		Optional<Order> orderOpt = orderRepository.findFirstByStatusOrderByCreatedAtAsc(OrderStatus.ACCEPTED_WAITING_COURIER);
		if (orderOpt.isEmpty()) {
			return false;
		}
		assignCourier(orderOpt.get(), courier);
		return true;
	}

	private void assignCourier(Order order, User courier) {
		OffsetDateTime now = OffsetDateTime.now(clock);
		order.setCourier(courier);
		order.setStatus(OrderStatus.COURIER_ASSIGNED);
		order.setCourierAssignedAt(now);
		courier.setAvailable(false);
		orderRepository.save(order);
		userRepository.save(courier);

		String distanceSuffix = "";
		if (shopAddress != null && !shopAddress.isBlank()
				&& order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank()) {
			try {
				Double km = yandexMapsEisService.calculateDistanceKm(shopAddress, order.getDeliveryAddress());
				if (km != null) {
					distanceSuffix = " (distance from shop: ~" + km + " km)";
				}
			} catch (LogisticsException e) {
				log.warn("Failed to calculate distance for order {}: {}", order.getId(), e.getMessage());
			}
		}

		
		notificationService.createNotification(
				courier,
				order,
				NotificationType.COURIER_ASSIGNED,
				"You have been assigned to order " + order.getId() + distanceSuffix
		);
		notificationService.createNotification(
				order.getCustomer(),
				order,
				NotificationType.COURIER_ASSIGNED,
				"Courier assigned to order " + order.getId()
		);
		notificationService.createNotification(
				order.getShop(),
				order,
				NotificationType.COURIER_ASSIGNED,
				"Courier assigned to order " + order.getId()
		);

		
		asyncEventPublisher.courierAssigned(order);

		log.info("Courier {} assigned to order {}", courier.getId(), order.getId());
	}
}
