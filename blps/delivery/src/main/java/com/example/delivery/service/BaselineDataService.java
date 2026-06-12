package com.example.delivery.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.model.ShopItem;
import com.example.delivery.model.User;
import com.example.delivery.model.UserRole;
import com.example.delivery.repository.ShopItemRepository;
import com.example.delivery.repository.UserRepository;

@Service
public class BaselineDataService {

	private final UserRepository userRepository;
	private final ShopItemRepository shopItemRepository;

	public BaselineDataService(UserRepository userRepository, ShopItemRepository shopItemRepository) {
		this.userRepository = userRepository;
		this.shopItemRepository = shopItemRepository;
	}

	@Transactional
	public void ensureDefaultData() {
		User shop = userRepository.findFirstByRoleOrderByIdAsc(UserRole.SHOP)
				.orElseGet(() -> userRepository.save(new User("Default Shop", UserRole.SHOP, false)));

		if (shopItemRepository.countByShopId(shop.getId()) == 0) {
			List<ShopItem> items = List.of(
					new ShopItem(shop, "Pizza Margherita", new BigDecimal("12.50"), true),
					new ShopItem(shop, "Cheeseburger", new BigDecimal("9.00"), true),
					new ShopItem(shop, "Greek Salad", new BigDecimal("7.50"), true)
			);
			shopItemRepository.saveAll(items);
		}
	}
}
