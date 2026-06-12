package com.example.delivery.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.NotificationResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.model.Notification;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.User;
import com.example.delivery.repository.NotificationRepository;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final Clock clock;

	public NotificationService(NotificationRepository notificationRepository, Clock clock) {
		this.notificationRepository = notificationRepository;
		this.clock = clock;
	}

	@Transactional
	public Notification createNotification(User recipient, Order order, NotificationType type, String message) {
		Notification notification = new Notification(recipient, order, type, message);
		return notificationRepository.save(notification);
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
