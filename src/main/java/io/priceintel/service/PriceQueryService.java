package io.priceintel.service;

import io.priceintel.dto.SkuComparisonItem;
import io.priceintel.dto.response.LatestPriceResponse;
import io.priceintel.dto.response.PriceHistoryResponse;
import io.priceintel.dto.PricePoint;
import io.priceintel.dto.response.PriceStatsResponse;
import io.priceintel.dto.response.SkuComparisonResponse;
import io.priceintel.entity.PriceSnapshot;
import io.priceintel.enums.Availability;
import io.priceintel.exception.PriceSnapshotNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public SkuComparisonResponse compareSkus(List<Long> skuIds, Boolean inStockOnly, String sortBy) {
        log.info("Comparing {} SKUs with filters: inStockOnly={}, sortBy={}",
                skuIds != null ? skuIds.size() : 0, inStockOnly, sortBy);

        // 1. Validate input
        validateComparisonInput(skuIds);

        // 2. Collect valid comparison items
        assert skuIds != null;
        List<SkuComparisonItem> validItems = collectValidComparisonItems(skuIds);

        // 3. Apply in-stock filtering if requested
        if (Boolean.TRUE.equals(inStockOnly)) {
            validItems = filterInStockItems(validItems);
        }

        // 4. Ensure at least 2 valid items remain (already validated in collect/filter)

        // 5. Assign rankings based on price (cheapest = rank 1) BEFORE sorting
        assignRankings(validItems);

        // 6. Calculate comparison metrics AFTER filtering
        ComparisonMetrics metrics = calculateComparisonMetrics(validItems);

        // 7. Find best value (cheapest in stock)
        Long bestValueSkuId = findBestValueSkuId(validItems);

        // 8. Apply sorting (does NOT affect ranking values)
        applySorting(validItems, sortBy);

        // 9. Build response
        SkuComparisonResponse response = SkuComparisonResponse.builder()
                .totalCompared(validItems.size())
                .cheapestSkuId(metrics.cheapestSkuId)
                .mostExpensiveSkuId(metrics.mostExpensiveSkuId)
                .bestValueSkuId(bestValueSkuId)
                .priceSpread(metrics.priceSpread)
                .percentageDifference(metrics.percentageDifference)
                .results(validItems)
                .build();

        log.info("Successfully compared {} SKUs - cheapest: {} ({}), mostExpensive: {} ({}), bestValue: {}, spread: {}, diff: {}%",
                validItems.size(), metrics.cheapestSkuId, metrics.minPrice,
                metrics.mostExpensiveSkuId, metrics.maxPrice, bestValueSkuId,
                metrics.priceSpread, metrics.percentageDifference);

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

    private void assignRankings(List<SkuComparisonItem> items) {
        // Sort by price ascending for ranking
        List<SkuComparisonItem> sortedForRanking = new ArrayList<>(items);
        sortedForRanking.sort(Comparator.comparing(SkuComparisonItem::getPrice));

        // Assign ranks
        for (int i = 0; i < sortedForRanking.size(); i++) {
            sortedForRanking.get(i).setRank(i + 1);
        }

        log.debug("Assigned rankings to {} items (rank 1 = cheapest)", items.size());
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

    private void applySorting(List<SkuComparisonItem> items, String sortBy) {
        Comparator<SkuComparisonItem> comparator = getComparator(sortBy);
        items.sort(comparator);

        String effectiveSortBy = sortBy != null ? sortBy.toLowerCase() : "price";
        log.debug("Sorted {} items by {}", items.size(), effectiveSortBy);
    }

    private Comparator<SkuComparisonItem> getComparator(String sortBy) {
        if (sortBy == null) {
            return Comparator.comparing(SkuComparisonItem::getPrice);
        }

        return switch (sortBy.toLowerCase()) {
            case "price" -> Comparator.comparing(SkuComparisonItem::getPrice);
            case "price_desc" -> Comparator.comparing(SkuComparisonItem::getPrice).reversed();
            case "latest" -> Comparator.comparing(SkuComparisonItem::getCapturedAt).reversed();
            default -> {
                log.debug("Unknown sortBy '{}', defaulting to price ascending", sortBy);
                yield Comparator.comparing(SkuComparisonItem::getPrice);
            }
        };
    }

    private void validateComparisonInput(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            log.warn("SKU IDs list is null or empty");
            throw new IllegalArgumentException("SKU IDs list cannot be null or empty");
        }

        if (skuIds.size() < 2) {
            log.warn("Insufficient SKU IDs provided for comparison: {}", skuIds.size());
            throw new IllegalArgumentException("At least 2 SKU IDs are required for comparison");
        }
    }

    private List<SkuComparisonItem> collectValidComparisonItems(List<Long> skuIds) {
        log.debug("Fetching latest snapshots for {} SKU IDs", skuIds.size());

        List<SkuComparisonItem> validItems = new ArrayList<>();

        for (Long skuId : skuIds) {
            // Validate each SKU ID
            try {
                validator.validateSkuId(skuId);
            } catch (IllegalArgumentException e) {
                log.debug("Skipping invalid SKU ID: {}", skuId);
                continue;
            }

            // Fetch latest snapshot
            Optional<PriceSnapshot> snapshotOpt = priceSnapshotService.getLatestSnapshot(skuId);

            if (snapshotOpt.isEmpty()) {
                log.debug("No snapshot found for skuId={}, skipping", skuId);
                continue;
            }

            PriceSnapshot snapshot = snapshotOpt.get();
            BigDecimal sellingPrice = snapshot.getSellingPrice();

            // Skip if selling price is null
            if (sellingPrice == null) {
                log.debug("Null selling price for skuId={}, skipping", skuId);
                continue;
            }

            // Add valid item
            SkuComparisonItem item = SkuComparisonItem.builder()
                    .skuId(skuId)
                    .price(sellingPrice)
                    .availability(snapshot.getAvailability())
                    .capturedAt(snapshot.getCapturedAt())
                    .build();

            validItems.add(item);
            log.debug("Added skuId={} with price={} to comparison", skuId, sellingPrice);
        }

        // Verify we have at least 2 valid SKUs
        if (validItems.size() < 2) {
            log.warn("Insufficient valid SKU snapshots for comparison. Required: 2, Found: {}", validItems.size());
            throw new IllegalArgumentException(
                    String.format("At least 2 valid SKU snapshots required for comparison. Found: %d", validItems.size())
            );
        }

        return validItems;
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

