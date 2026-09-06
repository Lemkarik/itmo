package com.example.delivery.scheduler;

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
import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;
import com.example.delivery.repository.OrderRepository;


@Component
@DisallowConcurrentExecution
public class CourierTimeoutJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CourierTimeoutJob.class);

    private final OrderRepository orderRepository;
    private final MessageProducerService messageProducerService;

    public CourierTimeoutJob(
            OrderRepository orderRepository,
            MessageProducerService messageProducerService) {
        this.orderRepository = orderRepository;
        this.messageProducerService = messageProducerService;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("CourierTimeoutJob triggered");

        try {
            retryCourierAssignments();
        } catch (Exception e) {
            log.error("Error in CourierTimeoutJob execution: {}", e.getMessage(), e);
            throw new JobExecutionException("Failed to process courier timeouts", e);
        }
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
