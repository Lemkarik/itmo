package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.NotificationResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.messaging.MessageProducerService;
import com.example.delivery.model.Notification;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.User;
import com.example.delivery.repository.NotificationRepository;

@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationRepository notificationRepository;
	private final Clock clock;
	private final MessageProducerService messageProducer;

	public NotificationService(
			NotificationRepository notificationRepository,
			Clock clock,
			MessageProducerService messageProducer) {
		this.notificationRepository = notificationRepository;
		this.clock = clock;
		this.messageProducer = messageProducer;
	}

	/**
	 * Creates a notification synchronously (direct DB write).
	 * Also sends an async message to the notification.dispatch queue
	 * for redundant delivery via other consumers.
	 */
	@Transactional
	public Notification createNotification(User recipient, Order order, NotificationType type, String message) {
		Notification notification = new Notification(recipient, order, type, message);
		Notification saved = notificationRepository.save(notification);

		// Also send asynchronously via JMS queue for reliability
		try {
			messageProducer.sendNotificationDispatch(
					recipient.getId(),
					order != null ? order.getId() : null,
					type.name(),
					message
			);
		} catch (Exception e) {
			log.warn("Failed to send async notification for user {}: {}",
					recipient.getId(), e.getMessage());
		}

		return saved;
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> listNotifications(Long userId, boolean unreadOnly, PaginationRequest pagination) {
		Page<Notification> notifications = unreadOnly
				? notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pagination.toPageable())
				: notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pagination.toPageable());
		return notifications.map(this::toResponse);
	}

	@Transactional
	public NotificationResponse markRead(Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
		if (notification.getReadAt() == null) {
			notification.setReadAt(OffsetDateTime.now(clock));
		}
		return toResponse(notificationRepository.save(notification));
	}

	private NotificationResponse toResponse(Notification notification) {
		Long orderId = notification.getOrder() != null ? notification.getOrder().getId() : null;
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getMessage(),
				notification.getCreatedAt(),
				notification.getReadAt(),
				orderId
		);
	}
}
