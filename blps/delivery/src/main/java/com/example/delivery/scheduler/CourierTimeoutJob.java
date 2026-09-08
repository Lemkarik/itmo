package com.example.delivery.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.messaging.MessageProducerService;
import com.example.delivery.model.NotificationType;
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.service.NotificationService;


@Component
@DisallowConcurrentExecution
public class CourierTimeoutJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CourierTimeoutJob.class);

    private static final Duration COURIER_ASSIGNMENT_TIMEOUT = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final MessageProducerService messageProducerService;
    private final Clock clock;

    public CourierTimeoutJob(
            OrderRepository orderRepository,
            NotificationService notificationService,
            MessageProducerService messageProducerService,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.messageProducerService = messageProducerService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("CourierTimeoutJob triggered at {}", OffsetDateTime.now(clock));

        try {
            
            retryCourierAssignments();

            
            OffsetDateTime now = OffsetDateTime.now(clock);
            OffsetDateTime threshold = now.minus(COURIER_ASSIGNMENT_TIMEOUT);
            List<Order> overdue = orderRepository.findByStatusAndAcceptedAtBefore(
                    OrderStatus.ACCEPTED_WAITING_COURIER, threshold);

            if (overdue.isEmpty()) {
                log.debug("No overdue orders for courier assignment timeout");
                return;
            }

            log.info("Found {} overdue orders for courier timeout", overdue.size());

            for (Order order : overdue) {
                processTimeout(order, now);
            }
        } catch (Exception e) {
            log.error("Error in CourierTimeoutJob execution: {}", e.getMessage(), e);
            throw new JobExecutionException("Failed to process courier timeouts", e);
        }
    }

    @Transactional
    protected void processTimeout(Order order, OffsetDateTime now) {
        if (order.getStatus() != OrderStatus.ACCEPTED_WAITING_COURIER) {
            return; 
        }

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

        log.info("Order {} cancelled - no courier found in time", order.getId());
    }

    @Transactional
    protected void retryCourierAssignments() {
        List<Order> waitingOrders = orderRepository.findByStatusOrderByCreatedAtAsc(
                OrderStatus.ACCEPTED_WAITING_COURIER);
        for (Order order : waitingOrders) {
            messageProducerService.sendCourierAssignmentRequest(order);
        }
    }
}
