package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.delivery.model.Order;


@Service
public class MessageProducerService {

    private static final Logger log = LoggerFactory.getLogger(MessageProducerService.class);

    private final JmsTemplate jmsTemplate;

    public MessageProducerService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendCourierAssignmentRequest(Order order) {
        OrderEventMessage msg = new OrderEventMessage(
                order.getId(),
                null,
                "New courier assignment request for order " + order.getId()
        );
        sendAfterCommit("courier.assignment", msg);
    }

    public void sendNotificationDispatch(Long recipientId, Long orderId,
                                          String type, String message) {
        OrderEventMessage msg = new OrderEventMessage(
                orderId, recipientId, message
        );
        sendAfterCommit("notification.dispatch", msg);
    }

    public void sendBitrixOrderSync(Order order) {
        OrderEventMessage message = new OrderEventMessage(
                order.getId(),
                null,
                "Synchronize order " + order.getId() + " with Bitrix24"
        );
        sendAfterCommit("bitrix.order.sync", message);
    }

    private void sendAfterCommit(String destination, OrderEventMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        send(destination, message);
                    } catch (RuntimeException exception) {
                        log.error("Failed to send {} message for order {} after transaction commit",
                                destination, message.getOrderId(), exception);
                    }
                }
            });
            return;
        }

        send(destination, message);
    }

    private void send(String destination, OrderEventMessage message) {
        jmsTemplate.convertAndSend(destination, message);
        log.info("Sent {} message for order {}", destination, message.getOrderId());
    }
}
