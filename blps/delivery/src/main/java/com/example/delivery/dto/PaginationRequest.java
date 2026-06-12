package com.example.delivery.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.delivery.exception.BadRequestException;

public class PaginationRequest {
	public static final String DEFAULT_PAGE_VALUE = "1";
	public static final String DEFAULT_PAGE_SIZE_VALUE = "20";
	public static final int MAX_PAGE_SIZE = 100;

	private final int page;
	private final int pageSize;

	public PaginationRequest(int page, int pageSize) {
		if (page < 1) {
			throw new BadRequestException("Page number must be greater than or equal to 1");
		}
		if (pageSize < 1) {
			throw new BadRequestException("Page size must be greater than or equal to 1");
		}
		if (pageSize > MAX_PAGE_SIZE) {
			throw new BadRequestException("Page size must be less than or equal to " + MAX_PAGE_SIZE);
		}
		this.page = page;
		this.pageSize = pageSize;
	}

	public Pageable toPageable() {
		return PageRequest.of(page - 1, pageSize);
	}

	public int getPage() {
		return page;
	}

	public int getPageSize() {
		return pageSize;
	}
}
