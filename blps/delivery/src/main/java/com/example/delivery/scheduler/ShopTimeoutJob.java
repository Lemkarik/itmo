package com.example.delivery.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.messaging.AsyncEventPublisher;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.service.NotificationService;


@Component
public class ShopTimeoutJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ShopTimeoutJob.class);

    private static final Duration SHOP_DECISION_TIMEOUT = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final AsyncEventPublisher asyncEventPublisher;
    private final Clock clock;

    public ShopTimeoutJob(
            OrderRepository orderRepository,
            NotificationService notificationService,
            AsyncEventPublisher asyncEventPublisher,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.asyncEventPublisher = asyncEventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("ShopTimeoutJob triggered at {}", OffsetDateTime.now(clock));

        try {
            OffsetDateTime now = OffsetDateTime.now(clock);
            OffsetDateTime threshold = now.minus(SHOP_DECISION_TIMEOUT);
            List<Order> overdue = orderRepository.findByStatusAndCreatedAtBefore(
                    OrderStatus.PENDING_SHOP, threshold);

            if (overdue.isEmpty()) {
                log.debug("No overdue orders for shop decision");
                return;
            }

            log.info("Found {} overdue orders for shop decision timeout", overdue.size());

            for (Order order : overdue) {
                processTimeout(order, now);
            }
        } catch (Exception e) {
            log.error("Error in ShopTimeoutJob execution: {}", e.getMessage(), e);
            throw new JobExecutionException("Failed to process shop timeouts", e);
        }
    }

    @Transactional
    protected void processTimeout(Order order, OffsetDateTime now) {
        if (order.getStatus() != OrderStatus.PENDING_SHOP) {
            return; 
        }

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

        asyncEventPublisher.orderTimedOut(order);

        log.info("Order {} timed out - shop did not respond in time", order.getId());
    }
}
