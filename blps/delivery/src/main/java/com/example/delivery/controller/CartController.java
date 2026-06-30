package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.config.JwtUserDetails;
import com.example.delivery.dto.AddCartItemRequest;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_CART')")
	public CartResponse getCart() {
		Long customerId = getCurrentUserId();
		return cartService.getCart(customerId);
	}

	@PostMapping("/items")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_CART')")
	public CartResponse addItem(@RequestBody AddCartItemRequest request) {
		Long customerId = getCurrentUserId();
		return cartService.addItem(customerId, request);
	}

	@DeleteMapping("/items/{itemId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_CART')")
	public CartResponse removeItem(@PathVariable Long itemId) {
		Long customerId = getCurrentUserId();
		return cartService.removeItem(customerId, itemId);
	}

	private Long getCurrentUserId() {
		return ((JwtUserDetails) SecurityContextHolder
				.getContext().getAuthentication().getPrincipal()).getUserId();
	}
}
