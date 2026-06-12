package com.example.delivery.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.delivery.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

	List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId);

	Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

	Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
}
