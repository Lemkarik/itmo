package com.example.delivery.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.delivery.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
	Optional<Cart> findByCustomerId(Long customerId);
}
