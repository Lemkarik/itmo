package com.example.delivery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.delivery.dto.ListResponse;
import com.example.delivery.dto.PaginationRequest;
import com.example.delivery.dto.ShopItemResponse;
import com.example.delivery.service.ShopItemService;

@RestController
@RequestMapping("/api/items")
public class ShopItemController {

	private final ShopItemService shopItemService;

	public ShopItemController(ShopItemService shopItemService) {
		this.shopItemService = shopItemService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PRIVILEGE_VIEW_ITEMS')")
	public ListResponse<ShopItemResponse> listItems(
			@RequestParam(name = "page", defaultValue = PaginationRequest.DEFAULT_PAGE_VALUE) int page,
			@RequestParam(name = "pageSize", defaultValue = PaginationRequest.DEFAULT_PAGE_SIZE_VALUE) int pageSize
	) {
		return new ListResponse<>(shopItemService.listActiveItems(new PaginationRequest(page, pageSize)));
	}
}
