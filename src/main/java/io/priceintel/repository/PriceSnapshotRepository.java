package io.priceintel.repository;

import io.priceintel.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
    //Get Full History For SKU
    List<PriceSnapshot> findBySkuLocationIdOrderByCapturedAtDesc(Long skuLocationId);
    //Get Latest Price For SKU
    Optional<PriceSnapshot> findTopBySkuLocationIdOrderByCapturedAtDesc(Long skuLocationId);
    //Get History Between Dates
    List<PriceSnapshot> findBySkuLocationIdAndCapturedAtBetweenOrderByCapturedAtAsc(
            Long skuLocationId,
            Instant start,
            Instant end
    );
}

