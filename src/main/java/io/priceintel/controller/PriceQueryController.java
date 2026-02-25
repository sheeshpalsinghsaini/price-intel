package io.priceintel.controller;

import io.priceintel.dto.response.LatestPriceResponse;
import io.priceintel.dto.response.PriceHistoryResponse;
import io.priceintel.service.PriceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/skus")
public class PriceQueryController {

    private final PriceQueryService priceQueryService;

    @GetMapping("/{skuId}/latest")
    public ResponseEntity<LatestPriceResponse> getLatestPrice(@PathVariable Long skuId) {
        return ResponseEntity.ok(priceQueryService.getLatestPrice(skuId));
    }

    @GetMapping("/{skuId}/history")
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @PathVariable Long skuId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(priceQueryService.getHistory(skuId, start, end, limit));
    }
}

