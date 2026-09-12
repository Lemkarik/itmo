package com.example.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.jca.Bitrix24IntegrationException;
import com.example.delivery.model.Order;
import com.example.delivery.repository.OrderRepository;
import com.example.delivery.service.BitrixOrderSyncService;

@Component
@ConditionalOnProperty(
        name = "app.features.listener.bitrix-order-sync",
        havingValue = "true",
        matchIfMissing = true)
public class BitrixOrderSyncListener {

    private static final Logger log = LoggerFactory.getLogger(BitrixOrderSyncListener.class);

    private final OrderRepository orderRepository;
    private final BitrixOrderSyncService bitrixOrderSyncService;

    public BitrixOrderSyncListener(
            OrderRepository orderRepository,
            BitrixOrderSyncService bitrixOrderSyncService) {
        this.orderRepository = orderRepository;
        this.bitrixOrderSyncService = bitrixOrderSyncService;
    }

    @JmsListener(
            destination = "bitrix.order.sync",
            containerFactory = "bitrixJmsListenerContainerFactory")
    @Transactional
    public void synchronizeOrder(OrderEventMessage message) {
        Order order = orderRepository.findById(message.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for Bitrix24 synchronization", message.getOrderId());
            return;
        }

        try {
            bitrixOrderSyncService.synchronize(order);
            orderRepository.save(order);
            log.info("Order {} synchronized with Bitrix24 CRM item {} at status {}",
                    order.getId(), order.getBitrixDealId(), order.getStatus());
        } catch (Bitrix24IntegrationException exception) {
            throw new IllegalStateException(
                    "Could not synchronize order " + order.getId() + " with Bitrix24",
                    exception);
        }
    }
}
