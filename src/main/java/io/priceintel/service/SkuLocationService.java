package io.priceintel.service;

import io.priceintel.entity.Platform;
import io.priceintel.entity.Product;
import io.priceintel.entity.SkuLocation;
import io.priceintel.exception.PlatformNotFoundException;
import io.priceintel.exception.ProductNotFoundException;
import io.priceintel.repository.PlatformRepository;
import io.priceintel.repository.ProductRepository;
import io.priceintel.repository.SkuLocationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkuLocationService {

    private final SkuLocationRepository skuLocationRepository;
    private final ProductRepository productRepository;
    private final PlatformRepository platformRepository;

    @Transactional
    public SkuLocation createOrGetSkuLocation(
            Long productId,
            Long platformId,
            String city,
            String productUrl
    ) {
        log.debug("Creating or getting SKU location: productId={}, platformId={}, city={}",
                productId, platformId, city);

        String normalizedCity = normalizeCity(city);
        String normalizedProductUrl = normalizeProductUrl(productUrl);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found: productId={}", productId);
                    return new ProductNotFoundException(productId);
                });

        Platform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> {
                    log.error("Platform not found: platformId={}", platformId);
                    return new PlatformNotFoundException(platformId);
                });

        Optional<SkuLocation> existing = skuLocationRepository
                .findByProductIdAndPlatformIdAndCity(productId, platformId, normalizedCity);

        if (existing.isPresent()) {
            SkuLocation skuLocation = existing.get();
            boolean needsUpdate = false;

            if (!skuLocation.isActive()) {
                log.info("Reactivating inactive SKU location: id={}", skuLocation.getId());
                skuLocation.setActive(true);
                needsUpdate = true;
            }

            if (!normalizedProductUrl.equals(skuLocation.getProductUrl())) {
                log.info("Updating product URL for SKU location: id={}", skuLocation.getId());
                skuLocation.setProductUrl(normalizedProductUrl);
                needsUpdate = true;
            }

            if (needsUpdate) {
                SkuLocation updated = skuLocationRepository.save(skuLocation);
                log.info("Updated SKU location: id={}", updated.getId());
                return updated;
            }

            log.info("Returning existing SKU location: id={}", skuLocation.getId());
            return skuLocation;
        }

        SkuLocation skuLocation = SkuLocation.builder()
                .product(product)
                .platform(platform)
                .city(normalizedCity)
                .productUrl(normalizedProductUrl)
                .isActive(true)
                .build();

        SkuLocation saved = skuLocationRepository.save(skuLocation);
        log.info("Created new SKU location: id={}, productId={}, platformId={}, city={}",
                saved.getId(), productId, platformId, normalizedCity);
        return saved;
    }

    public Optional<SkuLocation> getById(Long id) {
        return skuLocationRepository.findById(id);
    }

    public List<SkuLocation> getByProductId(Long productId) {
        return skuLocationRepository.findByProductId(productId);
    }

    public List<SkuLocation> getByCity(String city) {
        return skuLocationRepository.findByCityIgnoreCase(normalizeCity(city));
    }

    private String normalizeCity(String city) {
        if (city == null || city.trim().isBlank()) {
            log.warn("Invalid city: empty or null");
            throw new IllegalArgumentException("City cannot be empty");
        }
        return city.trim().toLowerCase();
    }

    private String normalizeProductUrl(String productUrl) {
        if (productUrl == null || productUrl.trim().isBlank()) {
            log.warn("Invalid product URL: empty or null");
            throw new IllegalArgumentException("Product URL cannot be empty");
        }
        return productUrl.trim();
    }
}
