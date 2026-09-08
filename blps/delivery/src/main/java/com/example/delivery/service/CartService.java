package com.example.delivery.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.AddCartItemRequest;
import com.example.delivery.dto.CartItemResponse;
import com.example.delivery.dto.CartResponse;
import com.example.delivery.exception.BadRequestException;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.model.Cart;
import com.example.delivery.model.CartItem;
import com.example.delivery.model.ShopItem;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.CartItemRepository;
import com.example.delivery.repository.CartRepository;
import com.example.delivery.repository.ShopItemRepository;

@Service
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ShopItemRepository shopItemRepository;
	private final UserService userService;

	public CartService(
			CartRepository cartRepository,
			CartItemRepository cartItemRepository,
			ShopItemRepository shopItemRepository,
			UserService userService
	) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.shopItemRepository = shopItemRepository;
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public CartResponse getCart(Long customerId) {
		User customer = userService.getUserWithRole(customerId, UserRole.CUSTOMER);
		Cart cart = cartRepository.findByCustomerId(customer.getId()).orElseGet(() -> new Cart(customer));
		List<CartItemResponse> items = cart.getItems().stream()
				.map(item -> new CartItemResponse(
						item.getShopItem().getId(),
						item.getProductName(),
						item.getQuantity(),
						item.getUnitPrice()
				))
				.toList();
		return new CartResponse(items);
	}

	@Transactional
	public CartResponse addItem(Long customerId, AddCartItemRequest request) {
		if (request == null) {
			throw new BadRequestException("Request body is required");
		}
		if (request.getShopItemId() == null) {
			throw new BadRequestException("shopItemId is required");
		}
		if (request.getQuantity() == null || request.getQuantity() <= 0) {
			throw new BadRequestException("Quantity must be positive");
		}

		User customer = userService.getUserWithRole(customerId, UserRole.CUSTOMER);
		ShopItem shopItem = shopItemRepository.findById(request.getShopItemId())
				.orElseThrow(() -> new NotFoundException("Shop item not found: " + request.getShopItemId()));
		if (!Boolean.TRUE.equals(shopItem.getActive())) {
			throw new BadRequestException("Shop item is not available: " + request.getShopItemId());
		}
		Cart cart = cartRepository.findByCustomerId(customer.getId()).orElseGet(() -> cartRepository.save(new Cart(customer)));

		CartItem existing = cart.getItems().stream()
				.filter(item -> item.getShopItem().getId().equals(shopItem.getId()))
				.findFirst()
				.orElse(null);

		if (existing != null) {
			try {
				existing.setQuantity(Math.addExact(existing.getQuantity(), request.getQuantity()));
			} catch (ArithmeticException ex) {
				throw new BadRequestException("Quantity is too large");
			}
			existing.setUnitPrice(shopItem.getPrice());
		} else {
			CartItem item = new CartItem(cart, shopItem, shopItem.getName(), request.getQuantity(), shopItem.getPrice());
			cart.getItems().add(item);
		}

		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Transactional
	public CartResponse removeItem(Long customerId, Long shopItemId) {
		User customer = userService.getUserWithRole(customerId, UserRole.CUSTOMER);
		Cart cart = cartRepository.findByCustomerId(customer.getId())
				.orElseThrow(() -> new NotFoundException("Cart not found for customer: " + customerId));
		CartItem item = cart.getItems().stream()
				.filter(cartItem -> cartItem.getShopItem().getId().equals(shopItemId))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("Product not found in cart: " + shopItemId));
		cart.getItems().remove(item);
		cartItemRepository.delete(item);
		return toResponse(cart);
	}

	@Transactional
	public void clearCart(Cart cart) {
		cart.getItems().clear();
		cartRepository.save(cart);
	}

	@Transactional(readOnly = true)
	public Cart getOrCreateCart(User customer) {
		return cartRepository.findByCustomerId(customer.getId())
				.orElseGet(() -> cartRepository.save(new Cart(customer)));
	}

	private CartResponse toResponse(Cart cart) {
		List<CartItemResponse> items = cart.getItems().stream()
				.map(item -> new CartItemResponse(
						item.getShopItem().getId(),
						item.getProductName(),
						item.getQuantity(),
						item.getUnitPrice()
				))
				.toList();
		return new CartResponse(items);
	}
}
