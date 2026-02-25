package io.priceintel.service;

import io.priceintel.entity.PriceSnapshot;
import io.priceintel.entity.SkuLocation;
import io.priceintel.enums.Availability;
import io.priceintel.enums.CrawlStatus;
import io.priceintel.exception.SkuLocationNotFoundException;
import io.priceintel.repository.PriceSnapshotRepository;
import io.priceintel.repository.SkuLocationRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceSnapshotService {

    private static final Duration DUPLICATE_THRESHOLD = Duration.ofMinutes(30);

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final SkuLocationRepository skuLocationRepository;

    @Transactional
    public PriceSnapshot recordPrice(
            Long skuLocationId,
            BigDecimal sellingPrice,
            BigDecimal discount,
            Availability availability,
            CrawlStatus crawlStatus,
            Instant capturedAt
    ) {
        log.debug("Recording price snapshot: skuLocationId={}, sellingPrice={}, availability={}, crawlStatus={}",
                skuLocationId, sellingPrice, availability, crawlStatus);

        validateInputs(skuLocationId, sellingPrice, discount, availability, crawlStatus, capturedAt);

        SkuLocation skuLocation = skuLocationRepository.findById(skuLocationId)
                .orElseThrow(() -> {
                    log.error("SKU location not found: skuLocationId={}", skuLocationId);
                    return new SkuLocationNotFoundException(skuLocationId);
                });

        Optional<PriceSnapshot> latestSnapshot = priceSnapshotRepository
                .findTopBySkuLocationIdOrderByCapturedAtDesc(skuLocationId);

        if (latestSnapshot.isPresent() && isDuplicate(latestSnapshot.get(), sellingPrice, discount, availability, crawlStatus, capturedAt)) {
            log.info("Duplicate price snapshot detected for skuLocationId={}, returning existing snapshot id={}",
                    skuLocationId, latestSnapshot.get().getId());
            return latestSnapshot.get();
        }

        PriceSnapshot priceSnapshot = PriceSnapshot.builder()
                .skuLocation(skuLocation)
                .sellingPrice(sellingPrice)
                .discount(discount)
                .availability(availability)
                .crawlStatus(crawlStatus)
                .capturedAt(capturedAt)
                .build();

        PriceSnapshot saved = priceSnapshotRepository.save(priceSnapshot);
        log.info("Recorded new price snapshot: id={}, skuLocationId={}, sellingPrice={}, availability={}",
                saved.getId(), skuLocationId, sellingPrice, availability);
        return saved;
    }

    public Optional<PriceSnapshot> getLatestSnapshot(Long skuLocationId) {
        return priceSnapshotRepository.findTopBySkuLocationIdOrderByCapturedAtDesc(skuLocationId);
    }

    public List<PriceSnapshot> getHistory(Long skuLocationId) {
        return priceSnapshotRepository.findBySkuLocationIdOrderByCapturedAtDesc(skuLocationId);
    }

    public List<PriceSnapshot> getHistoryBetween(Long skuLocationId, Instant start, Instant end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end timestamps cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start timestamp must be before end timestamp");
        }
        return priceSnapshotRepository.findBySkuLocationIdAndCapturedAtBetweenOrderByCapturedAtAsc(
                skuLocationId, start, end
        );
    }

    public List<PriceSnapshot> getLatestSnapshotsForSkuIds(List<Long> skuIds) {
        log.debug("Fetching latest snapshots for {} SKU IDs", skuIds != null ? skuIds.size() : 0);

        if (skuIds == null || skuIds.isEmpty()) {
            log.warn("SKU IDs list is null or empty");
            throw new IllegalArgumentException("SKU IDs list cannot be null or empty");
        }

        List<PriceSnapshot> snapshots = priceSnapshotRepository.findLatestSnapshotsForSkuIds(skuIds);
        log.debug("Fetched {} latest snapshots for {} SKU IDs", snapshots.size(), skuIds.size());

        return snapshots;
    }

    private void validateInputs(
            Long skuLocationId,
            BigDecimal sellingPrice,
            BigDecimal discount,
            Availability availability,
            CrawlStatus crawlStatus,
            Instant capturedAt
    ) {
        if (skuLocationId == null) {
            log.warn("Invalid input: SKU Location ID is null");
            throw new IllegalArgumentException("SKU Location ID cannot be null");
        }
        if (sellingPrice == null) {
            log.warn("Invalid input: Selling price is null");
            throw new IllegalArgumentException("Selling price cannot be null");
        }
        if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Invalid input: Selling price is negative: {}", sellingPrice);
            throw new IllegalArgumentException("Selling price cannot be negative");
        }
        if (discount != null && discount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Invalid input: Discount is negative: {}", discount);
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        if (availability == null) {
            log.warn("Invalid input: Availability is null");
            throw new IllegalArgumentException("Availability cannot be null");
        }
        if (crawlStatus == null) {
            log.warn("Invalid input: Crawl status is null");
            throw new IllegalArgumentException("Crawl status cannot be null");
        }
        if (capturedAt == null) {
            log.warn("Invalid input: Captured at timestamp is null");
            throw new IllegalArgumentException("Captured at timestamp cannot be null");
        }
    }


    private boolean isDuplicate(
            PriceSnapshot latest,
            BigDecimal sellingPrice,
            BigDecimal discount,
            Availability availability,
            CrawlStatus crawlStatus,
            Instant capturedAt
    ) {
        boolean priceMatches = latest.getSellingPrice().compareTo(sellingPrice) == 0;
        boolean discountMatches = areDiscountsEqual(latest.getDiscount(), discount);
        boolean availabilityMatches = latest.getAvailability() == availability;
        boolean crawlStatusMatches = latest.getCrawlStatus() == crawlStatus;
        boolean withinTimeWindow = Duration.between(latest.getCapturedAt(), capturedAt)
                .abs()
                .compareTo(DUPLICATE_THRESHOLD) <= 0;

        return priceMatches && discountMatches && availabilityMatches && crawlStatusMatches && withinTimeWindow;
    }

    private boolean areDiscountsEqual(BigDecimal discount1, BigDecimal discount2) {
        if (discount1 == null && discount2 == null) {
            return true;
        }
        if (discount1 == null || discount2 == null) {
            return false;
        }
        return discount1.compareTo(discount2) == 0;
    }
}

