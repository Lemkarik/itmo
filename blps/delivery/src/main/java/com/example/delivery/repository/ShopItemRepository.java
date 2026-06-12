package com.example.delivery.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.delivery.model.ShopItem;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
	List<ShopItem> findByActiveTrueOrderByIdAsc();

	Page<ShopItem> findByActiveTrueOrderByIdAsc(Pageable pageable);

	long countByShopId(Long shopId);
}
