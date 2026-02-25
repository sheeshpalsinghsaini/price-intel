package io.priceintel.service;

import io.priceintel.dto.response.LatestPriceResponse;
import io.priceintel.dto.response.PriceHistoryResponse;
import io.priceintel.dto.response.PricePoint;
import io.priceintel.entity.PriceSnapshot;
import io.priceintel.exception.PriceSnapshotNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

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
            snapshots = snapshots.reversed();
        }

        return snapshots;
    }

    private List<PricePoint> applyLimit(List<PricePoint> pricePoints, Integer limit) {
        if (limit != null && limit < pricePoints.size()) {
            log.debug("Applying limit={}, original size={}", limit, pricePoints.size());
            // Return the latest N records (take from end since list is chronological)
            return pricePoints.subList(pricePoints.size() - limit, pricePoints.size());
        }
        return pricePoints;
    }
}

