package com.example.delivery.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.dto.ShopItemResponse;
import com.example.delivery.exception.NotFoundException;
import com.example.delivery.model.ShopItem;
import com.example.delivery.repository.ShopItemRepository;

@Service
public class ShopItemService {

	private final ShopItemRepository shopItemRepository;

	public ShopItemService(ShopItemRepository shopItemRepository) {
		this.shopItemRepository = shopItemRepository;
	}

	@Transactional(readOnly = true)
	public Page<ShopItemResponse> listActiveItems(PaginationRequest pagination) {
		return shopItemRepository.findByActiveTrueOrderByIdAsc(pagination.toPageable())
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public ShopItem getItem(Long itemId) {
		return shopItemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Shop item not found: " + itemId));
	}

	private ShopItemResponse toResponse(ShopItem item) {
		return new ShopItemResponse(item.getId(), item.getName(), item.getPrice(), item.getActive());
	}
}
