package io.priceintel.crawler.facade;

import io.priceintel.crawler.dto.IngestionRequest;
import io.priceintel.entity.Platform;
import io.priceintel.entity.PriceSnapshot;
import io.priceintel.entity.Product;
import io.priceintel.entity.SkuLocation;
import io.priceintel.service.PlatformService;
import io.priceintel.service.PriceSnapshotService;
import io.priceintel.service.ProductService;
import io.priceintel.service.SkuLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionFacadeService {

    private final ProductService productService;
    private final PlatformService platformService;
    private final SkuLocationService skuLocationService;
    private final PriceSnapshotService priceSnapshotService;

    @Transactional
    public void ingest(IngestionRequest request) {
        if (request == null) {
            log.warn("Ingestion request is null");
            throw new IllegalArgumentException("Ingestion request cannot be null");
        }

        log.info("Starting ingestion: brand={}, product={}, platform={}, city={}, capturedAt={}",
                request.getBrandName(), request.getProductName(), request.getPlatformName(),
                request.getCity(), request.getCapturedAt());

        checkMissingFields(request);

        Product product = productService.createProduct(
                request.getBrandName(),
                request.getProductName(),
                request.getPackSize()
        );
        log.debug("Product resolved: id={}", product.getId());

        Platform platform = platformService.createPlatform(request.getPlatformName());
        log.debug("Platform resolved: id={}", platform.getId());

        SkuLocation skuLocation = skuLocationService.createOrGetSkuLocation(
                product.getId(),
                platform.getId(),
                request.getCity(),
                request.getProductUrl()
        );
        log.debug("SKU location resolved: id={}", skuLocation.getId());

        PriceSnapshot priceSnapshot = priceSnapshotService.recordPrice(
                skuLocation.getId(),
                request.getSellingPrice(),
                request.getDiscount(),
                request.getAvailability(),
                request.getCrawlStatus(),
                request.getCapturedAt()
        );

        log.info("Ingestion completed successfully: productId={}, platformId={}, skuLocationId={}, snapshotId={}, sellingPrice={}, availability={}, crawlStatus={}",
                product.getId(), platform.getId(), skuLocation.getId(), priceSnapshot.getId(),
                request.getSellingPrice(), request.getAvailability(), request.getCrawlStatus());
    }

    private void checkMissingFields(IngestionRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (request.getBrandName() == null || request.getBrandName().trim().isBlank()) {
            missingFields.add("brandName");
        }
        if (request.getProductName() == null || request.getProductName().trim().isBlank()) {
            missingFields.add("productName");
        }
        if (request.getPlatformName() == null || request.getPlatformName().trim().isBlank()) {
            missingFields.add("platformName");
        }
        if (request.getCity() == null || request.getCity().trim().isBlank()) {
            missingFields.add("city");
        }
        if (request.getSellingPrice() == null) {
            missingFields.add("sellingPrice");
        }
        if (request.getAvailability() == null) {
            missingFields.add("availability");
        }
        if (request.getCrawlStatus() == null) {
            missingFields.add("crawlStatus");
        }
        if (request.getCapturedAt() == null) {
            missingFields.add("capturedAt");
        }

        if (!missingFields.isEmpty()) {
            log.warn("Missing fields: {}", String.join(", ", missingFields));
        }
    }
}

