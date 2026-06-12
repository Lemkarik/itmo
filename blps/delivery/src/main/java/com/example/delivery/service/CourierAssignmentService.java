package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.repository.UserRepository;

@Service
public class CourierAssignmentService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final Clock clock;

	public CourierAssignmentService(
			OrderRepository orderRepository,
			UserRepository userRepository,
			NotificationService notificationService,
			Clock clock
	) {
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.clock = clock;
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

		notificationService.createNotification(
				courier,
				order,
				NotificationType.COURIER_ASSIGNED,
				"You have been assigned to order " + order.getId()
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
	}
}
