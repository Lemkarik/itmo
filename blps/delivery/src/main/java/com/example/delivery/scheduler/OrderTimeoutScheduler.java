package com.example.delivery.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.service.CourierAssignmentService;
import com.example.delivery.service.NotificationService;

@Component
public class OrderTimeoutScheduler {

	private static final Duration SHOP_DECISION_TIMEOUT = Duration.ofMinutes(10);
	private static final Duration COURIER_ASSIGNMENT_TIMEOUT = Duration.ofMinutes(10);

	private final OrderRepository orderRepository;
	private final NotificationService notificationService;
	private final CourierAssignmentService courierAssignmentService;
	private final Clock clock;

	public OrderTimeoutScheduler(
			OrderRepository orderRepository,
			NotificationService notificationService,
			CourierAssignmentService courierAssignmentService,
			Clock clock
	) {
		this.orderRepository = orderRepository;
		this.notificationService = notificationService;
		this.courierAssignmentService = courierAssignmentService;
		this.clock = clock;
	}

	@Scheduled(fixedDelay = 60_000)
	@Transactional
	public void handleShopTimeouts() {
		OffsetDateTime now = OffsetDateTime.now(clock);
		OffsetDateTime threshold = now.minus(SHOP_DECISION_TIMEOUT);
		List<Order> overdue = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_SHOP, threshold);
		for (Order order : overdue) {
			order.setStatus(OrderStatus.DECLINED_BY_SHOP);
			order.setShopRespondedAt(now);
			order.setCancelledAt(now);
			orderRepository.save(order);

			notificationService.createNotification(
					order.getCustomer(),
					order,
					NotificationType.SHOP_DECLINED,
					"Shop did not respond in time. Order " + order.getId() + " declined automatically"
			);
			notificationService.createNotification(
					order.getShop(),
					order,
					NotificationType.SHOP_DECLINED,
					"Order " + order.getId() + " declined automatically (no response in time)"
			);
		}
	}

	@Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
	@Transactional
	public void handleCourierTimeouts() {
		retryCourierAssignments();

		OffsetDateTime now = OffsetDateTime.now(clock);
		OffsetDateTime threshold = now.minus(COURIER_ASSIGNMENT_TIMEOUT);
		List<Order> overdue = orderRepository.findByStatusAndAcceptedAtBefore(OrderStatus.ACCEPTED_WAITING_COURIER, threshold);
		for (Order order : overdue) {
			order.setStatus(OrderStatus.CANCELLED_NO_COURIER);
			order.setCancelledAt(now);
			orderRepository.save(order);

			notificationService.createNotification(
					order.getCustomer(),
					order,
					NotificationType.COURIER_NOT_FOUND,
					"No courier found in time. Order " + order.getId() + " cancelled"
			);
			notificationService.createNotification(
					order.getShop(),
					order,
					NotificationType.COURIER_NOT_FOUND,
					"No courier found in time. Order " + order.getId() + " cancelled"
			);
		}
	}

	private void retryCourierAssignments() {
		List<Order> waitingOrders = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.ACCEPTED_WAITING_COURIER);
		for (Order order : waitingOrders) {
			boolean assigned = courierAssignmentService.tryAssignCourier(order);
			if (assigned) {
			}
		}
	}
}

