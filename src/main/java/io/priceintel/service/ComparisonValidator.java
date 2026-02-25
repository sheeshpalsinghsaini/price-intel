package io.priceintel.service;

import io.priceintel.enums.ComparisonSortType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ComparisonValidator {

    private static final int MAX_SKU_BATCH_SIZE = 2000;
    private static final int MAX_PAGE_SIZE = 100;

    public void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            log.warn("Invalid productId: {}", productId);
            throw new IllegalArgumentException("Product ID must be positive");
        }
    }

    public void validateSkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            log.warn("SKU IDs list is null or empty");
            throw new IllegalArgumentException("SKU IDs list cannot be null or empty");
        }

        if (skuIds.size() < 2) {
            log.warn("Insufficient SKU IDs provided for comparison: {}", skuIds.size());
            throw new IllegalArgumentException("At least 2 SKU IDs are required for comparison");
        }

        if (skuIds.size() > MAX_SKU_BATCH_SIZE) {
            log.warn("SKU batch size exceeds maximum: {} > {}", skuIds.size(), MAX_SKU_BATCH_SIZE);
            throw new IllegalArgumentException(
                    String.format("SKU batch size cannot exceed %d. Received: %d", MAX_SKU_BATCH_SIZE, skuIds.size())
            );
        }
    }

    public void validatePagination(Integer page, Integer size) {
        if (page != null && page < 0) {
            log.warn("Invalid page number: {}", page);
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size != null) {
            if (size <= 0) {
                log.warn("Invalid page size: {}", size);
                throw new IllegalArgumentException("Page size must be positive");
            }

            if (size > MAX_PAGE_SIZE) {
                log.warn("Page size exceeds maximum: {} > {}", size, MAX_PAGE_SIZE);
                throw new IllegalArgumentException(
                        String.format("Page size cannot exceed %d. Received: %d", MAX_PAGE_SIZE, size)
                );
            }
        }
    }

    public void validateSortType(ComparisonSortType sortType) {
        // Enum validation is automatic, but we can add logging
        if (sortType != null) {
            log.debug("Sort type validated: {}", sortType);
        }
    }
}

