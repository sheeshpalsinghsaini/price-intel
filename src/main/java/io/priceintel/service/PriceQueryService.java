package io.priceintel.service;

import io.priceintel.dto.SkuComparisonItem;
import io.priceintel.dto.response.LatestPriceResponse;
import io.priceintel.dto.response.PriceHistoryResponse;
import io.priceintel.dto.PricePoint;
import io.priceintel.dto.response.PriceStatsResponse;
import io.priceintel.dto.response.SkuComparisonResponse;
import io.priceintel.entity.PriceSnapshot;
import io.priceintel.entity.SkuLocation;
import io.priceintel.enums.Availability;
import io.priceintel.enums.ComparisonSortType;
import io.priceintel.exception.PriceSnapshotNotFoundException;
import io.priceintel.repository.SkuLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceQueryService {

    private final PriceSnapshotService priceSnapshotService;
    private final PriceSnapshotMapper priceSnapshotMapper;
    private final PriceQueryValidator validator;
    private final SkuLocationRepository skuLocationRepository;
    private final ComparisonValidator comparisonValidator;

    @Transactional(readOnly = true)
    public LatestPriceResponse getLatestPrice(Long skuId) {
        log.info("Fetching latest price for skuId={}", skuId);

        validator.validateSkuId(skuId);

        PriceSnapshot snapshot = priceSnapshotService.getLatestSnapshot(skuId)
                .orElseThrow(() -> {
                    log.warn("No price snapshot found for skuId={}", skuId);
                    return new PriceSnapshotNotFoundException(skuId);
                });

        LatestPriceResponse response = priceSnapshotMapper.toLatestPriceResponse(skuId, snapshot);

        log.info("Successfully retrieved latest price for skuId={}, price={}, availability={}",
                skuId, snapshot.getSellingPrice(), snapshot.getAvailability());

        return response;
    }

    @Transactional(readOnly = true)
    public PriceHistoryResponse getHistory(Long skuId, Instant start, Instant end, Integer limit) {
        log.info("Fetching price history for skuId={}, start={}, end={}, limit={}", skuId, start, end, limit);

        // Validate inputs
        validator.validateSkuId(skuId);
        validator.validateDateRange(start, end);
        validator.validateLimit(limit);

        // Fetch snapshots based on date range
        List<PriceSnapshot> snapshots = fetchSnapshots(skuId, start, end);

        log.debug("Fetched {} snapshots for skuId={}", snapshots.size(), skuId);

        if (snapshots.isEmpty()) {
            log.warn("No price history found for skuId={}", skuId);
            throw new PriceSnapshotNotFoundException(skuId);
        }

        // Map to PricePoint
        List<PricePoint> pricePoints = snapshots.stream()
                .map(priceSnapshotMapper::toPricePoint)
                .toList();

        // Apply limit if specified
        List<PricePoint> finalHistory = applyLimit(pricePoints, limit);

        PriceHistoryResponse response = PriceHistoryResponse.builder()
                .skuId(skuId)
                .count(finalHistory.size())
                .history(finalHistory)
                .build();

        log.info("Successfully retrieved price history for skuId={}, totalRecords={}, returnedRecords={}",
                skuId, snapshots.size(), finalHistory.size());

        return response;
    }

    private List<PriceSnapshot> fetchSnapshots(Long skuId, Instant start, Instant end) {
        List<PriceSnapshot> snapshots;

        if (start != null && end != null) {
            log.debug("Fetching history between start={} and end={} for skuId={}", start, end, skuId);
            snapshots = priceSnapshotService.getHistoryBetween(skuId, start, end);
            // getHistoryBetween returns ASC order (oldest → newest) - no reversal needed
        } else {
            log.debug("Fetching full history for skuId={}", skuId);
            snapshots = priceSnapshotService.getHistory(skuId);
            // getHistory returns DESC order (newest → oldest) - reverse to get chronological order
            List<PriceSnapshot> mutableList = new ArrayList<>(snapshots);
            Collections.reverse(mutableList);
            snapshots = mutableList;
        }

        return snapshots;
    }

    private List<PricePoint> applyLimit(List<PricePoint> pricePoints, Integer limit) {
        if (limit == null || limit >= pricePoints.size()) {
            return pricePoints;
        }

        log.debug("Applying limit={}, original size={}", limit, pricePoints.size());
        // Return the latest N records (take from end since list is chronological)
        return pricePoints.subList(pricePoints.size() - limit, pricePoints.size());
    }

    @Transactional(readOnly = true)
    public PriceStatsResponse getStats(Long skuId, Instant start, Instant end) {
        log.info("Fetching price statistics for skuId={}, start={}, end={}", skuId, start, end);

        // Validate inputs
        validator.validateSkuId(skuId);
        validator.validateDateRange(start, end);

        // Fetch snapshots based on date range
        List<PriceSnapshot> snapshots;
        if (start != null && end != null) {
            log.debug("Fetching history between start={} and end={} for stats calculation", start, end);
            snapshots = priceSnapshotService.getHistoryBetween(skuId, start, end);
        } else {
            log.debug("Fetching full history for stats calculation");
            snapshots = priceSnapshotService.getHistory(skuId);
        }

        // Check if we have data
        if (snapshots.isEmpty()) {
            log.warn("No price snapshots found for skuId={} to calculate stats", skuId);
            throw new PriceSnapshotNotFoundException(skuId);
        }

        // Single-pass calculation with null safety
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        BigDecimal totalPrice = BigDecimal.ZERO;
        Instant lowestSeenAt = null;
        Instant highestSeenAt = null;
        int validRecordCount = 0;

        for (PriceSnapshot snapshot : snapshots) {
            BigDecimal sellingPrice = snapshot.getSellingPrice();

            // Defensive null check
            if (sellingPrice == null) {
                continue;
            }

            validRecordCount++;
            totalPrice = totalPrice.add(sellingPrice);

            // Track min price and timestamp
            if (minPrice == null || sellingPrice.compareTo(minPrice) < 0) {
                minPrice = sellingPrice;
                lowestSeenAt = snapshot.getCapturedAt();
            }

            // Track max price and timestamp
            if (maxPrice == null || sellingPrice.compareTo(maxPrice) > 0) {
                maxPrice = sellingPrice;
                highestSeenAt = snapshot.getCapturedAt();
            }
        }

        // Verify we have valid price data
        if (validRecordCount == 0) {
            log.warn("No valid price snapshots found for skuId={} to calculate stats", skuId);
            throw new PriceSnapshotNotFoundException(skuId);
        }

        // Calculate average price
        BigDecimal averagePrice = totalPrice.divide(
                BigDecimal.valueOf(validRecordCount),
                2,
                RoundingMode.HALF_UP
        );

        log.debug("Stats calculated - min={}, max={}, avg={}, validRecords={}",
                minPrice, maxPrice, averagePrice, validRecordCount);

        // Build response
        PriceStatsResponse response = PriceStatsResponse.builder()
                .skuId(skuId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .averagePrice(averagePrice)
                .lowestSeenAt(lowestSeenAt)
                .highestSeenAt(highestSeenAt)
                .totalRecords(validRecordCount)
                .build();

        log.info("Successfully calculated stats for skuId={}, minPrice={}, maxPrice={}, avgPrice={}, totalRecords={}",
                skuId, response.getMinPrice(), response.getMaxPrice(), response.getAveragePrice(), response.getTotalRecords());

        return response;
    }

    @Transactional(readOnly = true)
    public SkuComparisonResponse compareSkus(List<Long> skuIds, Boolean inStockOnly, ComparisonSortType sortType) {
        long startTime = System.currentTimeMillis();
        log.info("Comparing {} SKUs with filters: inStockOnly={}, sortType={}",
                skuIds != null ? skuIds.size() : 0, inStockOnly, sortType);

        // 1. Validate input using ComparisonValidator
        comparisonValidator.validateSkuIds(skuIds);

        // 2. Collect valid comparison items
        assert skuIds != null;
        List<SkuComparisonItem> validItems = collectValidComparisonItems(skuIds);

        // 3. Build and return comparison response
        SkuComparisonResponse response = buildComparisonResponse(validItems, inStockOnly, sortType, null, null);

        long duration = System.currentTimeMillis() - startTime;
        log.info("SKU comparison completed in {} ms", duration);

        return response;
    }

    @Transactional(readOnly = true)
    public SkuComparisonResponse compareProduct(Long productId, String city, Boolean inStockOnly, ComparisonSortType sortType, Integer page, Integer size) {
        long startTime = System.currentTimeMillis();
        log.info("Comparing product prices for productId={}, city={}, inStockOnly={}, sortType={}, page={}, size={}",
                productId, city, inStockOnly, sortType, page, size);

        // 1. Validate inputs
        comparisonValidator.validateProductId(productId);
        comparisonValidator.validatePagination(page, size);
        comparisonValidator.validateSortType(sortType);

        // 2. Fetch SKU locations
        List<SkuLocation> skuLocations;
        if (city != null && !city.trim().isEmpty()) {
            log.debug("Fetching active SKU locations for productId={} in city={}", productId, city);
            skuLocations = skuLocationRepository.findByProductIdAndCityIgnoreCaseAndIsActiveTrue(productId, city.trim());
        } else {
            log.debug("Fetching all active SKU locations for productId={}", productId);
            skuLocations = skuLocationRepository.findByProductIdAndIsActiveTrue(productId);
        }

        // 3. Check if SKUs found
        if (skuLocations.isEmpty()) {
            String errorMsg = city != null
                    ? String.format("No active SKU locations found for productId=%d in city=%s", productId, city)
                    : String.format("No active SKU locations found for productId=%d", productId);
            log.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.debug("Found {} active SKU locations for product comparison", skuLocations.size());

        // 4. Extract SKU IDs and remove duplicates
        List<Long> skuIds = skuLocations.stream()
                .map(SkuLocation::getId)
                .distinct()
                .toList();

        // 5. Validate SKU batch size using centralized validator
        comparisonValidator.validateSkuIds(skuIds);

        // 6. Batch fetch latest snapshots (eliminates N+1 query)
        log.debug("Batch fetching latest snapshots for {} SKUs", skuIds.size());
        List<PriceSnapshot> snapshots = priceSnapshotService.getLatestSnapshotsForSkuIds(skuIds);
        log.debug("Fetched {} snapshots out of {} SKUs", snapshots.size(), skuIds.size());

        // 7. Convert snapshots to comparison items
        List<SkuComparisonItem> validItems = convertSnapshotsToComparisonItems(snapshots);

        // 8. Build and return comparison response with pagination
        SkuComparisonResponse response = buildComparisonResponse(validItems, inStockOnly, sortType, page, size);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Product comparison completed in {} ms", duration);

        return response;
    }

    private List<SkuComparisonItem> filterInStockItems(List<SkuComparisonItem> items) {
        List<SkuComparisonItem> inStockItems = items.stream()
                .filter(item -> item.getAvailability() == Availability.IN_STOCK)
                .toList();

        log.debug("Filtered to {} in-stock items from {} total items", inStockItems.size(), items.size());

        if (inStockItems.size() < 2) {
            log.warn("Insufficient in-stock SKUs for comparison. Required: 2, Found: {}", inStockItems.size());
            throw new IllegalArgumentException(
                    String.format("At least 2 in-stock SKU snapshots required for comparison. Found: %d", inStockItems.size())
            );
        }

        return inStockItems;
    }

    private SkuComparisonResponse buildComparisonResponse(
            List<SkuComparisonItem> validItems,
            Boolean inStockOnly,
            ComparisonSortType sortType,
            Integer page,
            Integer size
    ) {
        // 1. Apply in-stock filtering if requested
        if (Boolean.TRUE.equals(inStockOnly)) {
            validItems = filterInStockItems(validItems);
        }

        int totalItems = validItems.size();

        // 2. Assign rankings based on price (cheapest = rank 1) BEFORE sorting
        validItems = assignRankings(validItems);

        // 3. Calculate comparison metrics AFTER filtering
        ComparisonMetrics metrics = calculateComparisonMetrics(validItems);

        // 4. Find best value (cheapest in stock)
        Long bestValueSkuId = findBestValueSkuId(validItems);

        // 5. Default sortType if null
        sortType = (sortType != null) ? sortType : ComparisonSortType.PRICE_ASC;

        // 6. Apply sorting (does NOT affect ranking values)
        validItems = applySorting(validItems, sortType);

        // 7. Apply pagination if requested (safe defaults)
        List<SkuComparisonItem> paginatedItems;
        Integer effectivePage = null;
        Integer effectiveSize = null;
        Integer totalPages = null;

        if (page != null) {
            // Pagination requested
            effectivePage = page;
            effectiveSize = (size != null) ? Math.min(size, 100) : 20; // Default 20, max 100
            totalPages = (totalItems + effectiveSize - 1) / effectiveSize;
            paginatedItems = applyPagination(validItems, effectivePage, effectiveSize);
        } else {
            // No pagination - return all items
            paginatedItems = validItems;
        }

        // 8. Build response
        SkuComparisonResponse response = SkuComparisonResponse.builder()
                .totalCompared(paginatedItems.size())
                .cheapestSkuId(metrics.cheapestSkuId)
                .mostExpensiveSkuId(metrics.mostExpensiveSkuId)
                .bestValueSkuId(bestValueSkuId)
                .priceSpread(metrics.priceSpread)
                .percentageDifference(metrics.percentageDifference)
                .results(paginatedItems)
                .page(effectivePage)
                .size(effectiveSize)
                .totalPages(totalPages)
                .totalItems(page != null ? totalItems : null)
                .build();

        log.info("Comparison completed: totalItems={}, returnedItems={}, page={}, size={}, cheapest={} ({}), mostExpensive={} ({}), bestValue={}, spread={}, diff={}%",
                totalItems, paginatedItems.size(), effectivePage, effectiveSize,
                metrics.cheapestSkuId, metrics.minPrice, metrics.mostExpensiveSkuId, metrics.maxPrice,
                bestValueSkuId, metrics.priceSpread, metrics.percentageDifference);

        return response;
    }

    private List<SkuComparisonItem> convertSnapshotsToComparisonItems(List<PriceSnapshot> snapshots) {
        List<SkuComparisonItem> items = new ArrayList<>();

        for (PriceSnapshot snapshot : snapshots) {
            BigDecimal sellingPrice = snapshot.getSellingPrice();

            // Skip if selling price is null
            if (sellingPrice == null) {
                log.debug("Null selling price for skuId={}, skipping", snapshot.getSkuLocation().getId());
                continue;
            }

            // Build comparison item
            SkuComparisonItem item = SkuComparisonItem.builder()
                    .skuId(snapshot.getSkuLocation().getId())
                    .price(sellingPrice)
                    .availability(snapshot.getAvailability())
                    .capturedAt(snapshot.getCapturedAt())
                    .build();

            items.add(item);
        }

        // Validate minimum count
        if (items.size() < 2) {
            log.warn("Insufficient valid SKU snapshots for comparison. Required: 2, Found: {}", items.size());
            throw new IllegalArgumentException(
                    String.format("At least 2 valid SKU snapshots required for comparison. Found: %d", items.size())
            );
        }

        return items;
    }

    private List<SkuComparisonItem> assignRankings(List<SkuComparisonItem> items) {
        // Sort by price ascending for ranking, with tie-breaker on capturedAt (latest first)
        List<SkuComparisonItem> sortedForRanking = new ArrayList<>(items);
        sortedForRanking.sort(Comparator.comparing(SkuComparisonItem::getPrice)
                .thenComparing(SkuComparisonItem::getCapturedAt, Comparator.reverseOrder()));

        // Assign ranks using toBuilder since DTOs are immutable
        List<SkuComparisonItem> rankedItems = new ArrayList<>();
        for (int i = 0; i < sortedForRanking.size(); i++) {
            SkuComparisonItem item = sortedForRanking.get(i);
            SkuComparisonItem rankedItem = item.toBuilder()
                    .rank(i + 1)
                    .build();
            rankedItems.add(rankedItem);
        }

        log.debug("Assigned rankings to {} items (rank 1 = cheapest)", items.size());
        return rankedItems;
    }

    private Long findBestValueSkuId(List<SkuComparisonItem> items) {
        Optional<SkuComparisonItem> bestValue = items.stream()
                .filter(item -> item.getAvailability() == Availability.IN_STOCK)
                .min(Comparator.comparing(SkuComparisonItem::getPrice));

        if (bestValue.isPresent()) {
            log.debug("Best value SKU (cheapest in-stock): skuId={}, price={}",
                    bestValue.get().getSkuId(), bestValue.get().getPrice());
            return bestValue.get().getSkuId();
        }

        log.debug("No in-stock items available, bestValueSkuId set to null");
        return null;
    }

    private List<SkuComparisonItem> applySorting(List<SkuComparisonItem> items, ComparisonSortType sortType) {
        Comparator<SkuComparisonItem> comparator = getComparator(sortType);
        List<SkuComparisonItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort(comparator);

        log.debug("Sorted {} items by {}", items.size(), sortType);
        return sortedItems;
    }

    private Comparator<SkuComparisonItem> getComparator(ComparisonSortType sortType) {
        return switch (sortType) {
            case PRICE_ASC -> Comparator.comparing(SkuComparisonItem::getPrice);
            case PRICE_DESC -> Comparator.comparing(SkuComparisonItem::getPrice).reversed();
            case LATEST -> Comparator.comparing(SkuComparisonItem::getCapturedAt).reversed();
        };
    }

    private List<SkuComparisonItem> applyPagination(List<SkuComparisonItem> items, Integer page, Integer size) {
        // Defensive checks
        if (page == null || page < 0 || size == null || size <= 0) {
            log.debug("Invalid pagination params: page={}, size={}, returning empty list", page, size);
            return new ArrayList<>();
        }

        if (size >= items.size()) {
            return new ArrayList<>(items);
        }

        int startIndex = page * size;
        if (startIndex >= items.size()) {
            log.debug("Page {} exceeds available items, returning empty list", page);
            return new ArrayList<>();
        }

        int endIndex = Math.min(startIndex + size, items.size());
        log.debug("Applying pagination: page={}, size={}, returning items [{} to {}]", page, size, startIndex, endIndex - 1);

        // Return detached list to avoid view-backed list issues
        return new ArrayList<>(items.subList(startIndex, endIndex));
    }

    private List<SkuComparisonItem> collectValidComparisonItems(List<Long> skuIds) {
        log.debug("Batch fetching latest snapshots for {} SKU IDs", skuIds.size());

        // Validate each SKU ID first
        List<Long> validSkuIds = new ArrayList<>();
        for (Long skuId : skuIds) {
            try {
                validator.validateSkuId(skuId);
                validSkuIds.add(skuId);
            } catch (IllegalArgumentException e) {
                log.debug("Skipping invalid SKU ID: {}", skuId);
            }
        }

        if (validSkuIds.isEmpty()) {
            log.warn("No valid SKU IDs after validation");
            throw new IllegalArgumentException("No valid SKU IDs provided");
        }

        // Batch fetch all latest snapshots in ONE query (eliminates N+1)
        List<PriceSnapshot> snapshots = priceSnapshotService.getLatestSnapshotsForSkuIds(validSkuIds);
        log.debug("Fetched {} snapshots out of {} valid SKU IDs", snapshots.size(), validSkuIds.size());

        // Convert to comparison items
        return convertSnapshotsToComparisonItems(snapshots);
    }

    private ComparisonMetrics calculateComparisonMetrics(List<SkuComparisonItem> validItems) {
        log.debug("Processing comparison for {} valid SKUs", validItems.size());

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        Long cheapestSkuId = null;
        Long mostExpensiveSkuId = null;

        // Find min and max prices in single pass
        for (SkuComparisonItem item : validItems) {
            BigDecimal price = item.getPrice();

            // Track minimum
            if (minPrice == null || price.compareTo(minPrice) < 0) {
                minPrice = price;
                cheapestSkuId = item.getSkuId();
            }

            // Track maximum
            if (maxPrice == null || price.compareTo(maxPrice) > 0) {
                maxPrice = price;
                mostExpensiveSkuId = item.getSkuId();
            }
        }

        // Defensive null check
        if (minPrice == null || maxPrice == null) {
            throw new IllegalStateException("Unexpected null price during comparison calculation");
        }

        // Calculate price spread
        BigDecimal priceSpread = maxPrice.subtract(minPrice);

        // Calculate percentage difference with division by zero protection
        BigDecimal percentageDifference;
        if (minPrice.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Min price is zero, setting percentageDifference to zero to avoid division by zero");
            percentageDifference = BigDecimal.ZERO;
        } else {
            percentageDifference = priceSpread
                    .divide(minPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        log.debug("Comparison calculated - cheapest={} ({}), mostExpensive={} ({}), spread={}, diff={}%",
                cheapestSkuId, minPrice, mostExpensiveSkuId, maxPrice, priceSpread, percentageDifference);

        return new ComparisonMetrics(minPrice, maxPrice, cheapestSkuId, mostExpensiveSkuId, priceSpread, percentageDifference);
    }

    private static class ComparisonMetrics {
        private final BigDecimal minPrice;
        private final BigDecimal maxPrice;
        private final Long cheapestSkuId;
        private final Long mostExpensiveSkuId;
        private final BigDecimal priceSpread;
        private final BigDecimal percentageDifference;

        public ComparisonMetrics(BigDecimal minPrice, BigDecimal maxPrice, Long cheapestSkuId,
                                 Long mostExpensiveSkuId, BigDecimal priceSpread, BigDecimal percentageDifference) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.cheapestSkuId = cheapestSkuId;
            this.mostExpensiveSkuId = mostExpensiveSkuId;
            this.priceSpread = priceSpread;
            this.percentageDifference = percentageDifference;
        }
    }
}

