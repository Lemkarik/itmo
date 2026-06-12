package com.example.delivery.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public class ListResponse<T> {
	private final List<T> items;
	private final int count;
	private final int page;
	private final int pageSize;
	private final long totalItems;
	private final int totalPages;

	public ListResponse(Page<T> page) {
		this.items = page.getContent();
		this.count = this.items.size();
		this.page = page.getNumber() + 1;
		this.pageSize = page.getSize();
		this.totalItems = page.getTotalElements();
		this.totalPages = page.getTotalPages();
	}

	public List<T> getItems() {
		return items;
	}

	public int getCount() {
		return count;
	}

	public int getPage() {
		return page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public long getTotalItems() {
		return totalItems;
	}

	public int getTotalPages() {
		return totalPages;
	}
}
