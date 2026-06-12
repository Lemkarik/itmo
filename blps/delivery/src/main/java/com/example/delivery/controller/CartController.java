package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.AddCartItemRequest;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.service.AuthService;
import com.example.delivery.service.CartService;

@RestController
@RequestMapping("/api/customers/{customerId}/cart")
public class CartController {

	private final CartService cartService;
	private final AuthService authService;

	public CartController(CartService cartService, AuthService authService) {
		this.cartService = cartService;
		this.authService = authService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_CART') and @authSecurity.isCurrentUser(#customerId)")
	public CartResponse getCart(@PathVariable Long customerId) {
		return cartService.getCart(customerId);
	}

	@PostMapping("/items")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_CART') and @authSecurity.isCurrentUser(#customerId)")
	public CartResponse addItem(@PathVariable Long customerId, @RequestBody AddCartItemRequest request) {
		return cartService.addItem(customerId, request);
	}

	@DeleteMapping("/items/{itemId}")
	@PreAuthorize("hasAuthority('PRIVILEGE_MANAGE_CART') and @authSecurity.isCurrentUser(#customerId)")
	public CartResponse removeItem(@PathVariable Long customerId, @PathVariable Long itemId) {
		return cartService.removeItem(customerId, itemId);
	}
}
