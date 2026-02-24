package io.priceintel.repository;

import io.priceintel.entity.SkuLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkuLocationRepository extends JpaRepository<SkuLocation, Long> {

    Optional<SkuLocation> findByProductIdAndPlatformIdAndCity(
            Long productId,
            Long platformId,
            String city
    );

    boolean existsByProductIdAndPlatformIdAndCity(
            Long productId,
            Long platformId,
            String city
    );

    List<SkuLocation> findByCityIgnoreCase(String city);

    List<SkuLocation> findByProductId(Long productId);
}
