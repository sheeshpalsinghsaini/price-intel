package io.priceintel.service;

import io.priceintel.dto.response.LatestPriceResponse;
import io.priceintel.dto.response.PricePoint;
import io.priceintel.entity.PriceSnapshot;
import org.springframework.stereotype.Component;

@Component
public class PriceSnapshotMapper {

    public PricePoint toPricePoint(PriceSnapshot snapshot) {
        return PricePoint.builder()
                .sellingPrice(snapshot.getSellingPrice())
                .discount(snapshot.getDiscount())
                .availability(snapshot.getAvailability())
                .capturedAt(snapshot.getCapturedAt())
                .build();
    }

    public LatestPriceResponse toLatestPriceResponse(Long skuId, PriceSnapshot snapshot) {
        return LatestPriceResponse.builder()
                .skuId(skuId)
                .sellingPrice(snapshot.getSellingPrice())
                .discount(snapshot.getDiscount())
                .availability(snapshot.getAvailability())
                .capturedAt(snapshot.getCapturedAt())
                .build();
    }
}

