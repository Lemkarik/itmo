package com.example.delivery.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.delivery.model.Order;
import com.example.delivery.model.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, OffsetDateTime threshold);

	List<Order> findByStatusAndAcceptedAtBefore(OrderStatus status, OffsetDateTime threshold);

	Optional<Order> findFirstByStatusOrderByCreatedAtAsc(OrderStatus status);

	List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

	List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

	List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);

	List<Order> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, OrderStatus status);

	List<Order> findByCourierIdOrderByCreatedAtDesc(Long courierId);

	Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

	Page<Order> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

	Page<Order> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, OrderStatus status, Pageable pageable);

	Page<Order> findByCourierIdOrderByCreatedAtDesc(Long courierId, Pageable pageable);
}

