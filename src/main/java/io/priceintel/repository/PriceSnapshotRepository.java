package io.priceintel.repository;

import io.priceintel.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    //Batch fetch latest snapshots for multiple SKU IDs (eliminates N+1 query)
    @Query("""
    SELECT ps
    FROM PriceSnapshot ps
    JOIN FETCH ps.skuLocation sl
    WHERE sl.id IN :skuIds
    AND ps.capturedAt = (
        SELECT MAX(ps2.capturedAt)
        FROM PriceSnapshot ps2
        WHERE ps2.skuLocation.id = sl.id
    )
    """)
    List<PriceSnapshot> findLatestSnapshotsForSkuIds(@Param("skuIds") List<Long> skuIds);
}

