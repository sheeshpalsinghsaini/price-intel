package io.priceintel.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class PriceQueryValidator {

    public void validateSkuId(Long skuId) {
        if (skuId == null || skuId <= 0) {
            log.warn("Invalid skuId: {}", skuId);
            throw new IllegalArgumentException("SKU ID must be a positive number");
        }
    }

    public void validateDateRange(Instant start, Instant end) {
        // Both must be provided or neither
        if ((start != null && end == null) || (start == null && end != null)) {
            log.warn("Invalid date range: start={}, end={}", start, end);
            throw new IllegalArgumentException("Both start and end parameters must be provided together");
        }

        // If both provided, start must be before or equal to end
        if (start != null && start.isAfter(end)) {
            log.warn("Invalid date range: start={} is after end={}", start, end);
            throw new IllegalArgumentException("Start timestamp must be before or equal to end timestamp");
        }
    }

    public void validateLimit(Integer limit) {
        if (limit != null && limit <= 0) {
            log.warn("Invalid limit: {}", limit);
            throw new IllegalArgumentException("Limit must be a positive number");
        }
    }
}

