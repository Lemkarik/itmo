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
import com.example.delivery.config.XmlUserDetailsService;

@Service
public class BaselineDataService {

	private final UserRepository userRepository;
	private final ShopItemRepository shopItemRepository;
	private final XmlUserDetailsService xmlUserDetailsService;

	public BaselineDataService(
			UserRepository userRepository,
			ShopItemRepository shopItemRepository,
			XmlUserDetailsService xmlUserDetailsService) {
		this.userRepository = userRepository;
		this.shopItemRepository = shopItemRepository;
		this.xmlUserDetailsService = xmlUserDetailsService;
	}

	@Transactional
	public void ensureDefaultData() {
		xmlUserDetailsService.getAllUsers().forEach(xmlUser -> {
			UserRole role = UserRole.valueOf(xmlUser.role());
			User user = userRepository.findByUsername(xmlUser.username())
					.orElseGet(() -> findUnlinkedUserOrCreate(role));
			boolean newAccountLink = user.getUsername() == null;
			user.setUsername(xmlUser.username());
			user.setName(xmlUser.username());
			user.setRole(role);
			if (newAccountLink && role == UserRole.COURIER) {
				user.setAvailable(true);
			}
			userRepository.save(user);
		});

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

	private User findUnlinkedUserOrCreate(UserRole role) {
		if (role == UserRole.SHOP) {
			return userRepository.findFirstByRoleAndUsernameIsNullOrderByIdAsc(role)
					.orElseGet(() -> new User("Default Shop", role, false));
		}
		return new User(role.name(), role, false);
	}
}
