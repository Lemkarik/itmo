package com.example.delivery.messaging;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.Notification;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.User;
import com.example.delivery.repository.NotificationRepository;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.repository.UserRepository;


@Component
@ConditionalOnProperty(name = "app.features.listener.notification-dispatch", havingValue = "true", matchIfMissing = true)
public class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public NotificationDispatchListener(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @JmsListener(destination = "notification.dispatch", concurrency = "1-3")
    @Transactional
    public void handleNotificationDispatch(OrderEventMessage message) {
        log.info("Received notification.dispatch message for order {}, recipient {}",
                message.getOrderId(), message.getCustomerId());

        try {
            User recipient = userRepository.findById(message.getCustomerId()).orElse(null);
            if (recipient == null) {
                log.warn("Recipient {} not found for notification", message.getCustomerId());
                return;
            }

            Order order = null;
            if (message.getOrderId() != null) {
                order = orderRepository.findById(message.getOrderId()).orElse(null);
            }

            
            NotificationType type = inferNotificationType(message.getMessage());

            Notification notification = new Notification(recipient, order, type, message.getMessage());
            notificationRepository.save(notification);
            log.info("Created notification for user {}: {}", recipient.getId(), message.getMessage());
        } catch (Exception e) {
            log.error("Error processing notification dispatch: {}", e.getMessage(), e);
        }
    }

    private NotificationType inferNotificationType(String message) {
        if (message == null) {
            return NotificationType.ORDER_CREATED;
        }
        String lower = message.toLowerCase();
        if (lower.contains("assigned")) {
            return NotificationType.COURIER_ASSIGNED;
        }
        if (lower.contains("accepted")) {
            return NotificationType.SHOP_ACCEPTED;
        }
        if (lower.contains("declined") || lower.contains("not respond")) {
            return NotificationType.SHOP_DECLINED;
        }
        if (lower.contains("no courier") || lower.contains("not found")) {
            return NotificationType.COURIER_NOT_FOUND;
        }
        if (lower.contains("cancelled")) {
            return NotificationType.SYSTEM_CANCELLED;
        }
        return NotificationType.ORDER_CREATED;
    }
}
