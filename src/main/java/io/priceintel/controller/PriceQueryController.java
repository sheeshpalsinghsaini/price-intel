package io.priceintel.controller;

import io.priceintel.dto.response.LatestPriceResponse;
import io.priceintel.dto.response.PriceHistoryResponse;
import io.priceintel.dto.response.SkuComparisonResponse;
import io.priceintel.service.PriceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/skus")
@Tag(name = "Price Query", description = "APIs for querying price data and comparisons")
public class PriceQueryController {

    private final PriceQueryService priceQueryService;

    @Operation(
            summary = "Get latest price for a SKU",
            description = "Retrieves the most recent price snapshot for the specified SKU location"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest price retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No price snapshot found for the SKU",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{skuId}/latest")
    public ResponseEntity<LatestPriceResponse> getLatestPrice(
            @Parameter(description = "SKU location ID", example = "1", required = true)
            @PathVariable Long skuId
    ) {
        return ResponseEntity.ok(priceQueryService.getLatestPrice(skuId));
    }

    @Operation(
            summary = "Get price history for a SKU",
            description = "Retrieves price history for the specified SKU location with optional date range and limit"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No price history found for the SKU",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{skuId}/history")
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @Parameter(description = "SKU location ID", example = "1", required = true)
            @PathVariable Long skuId,
            @Parameter(description = "Start date-time (ISO 8601 format)", example = "2026-02-01T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,
            @Parameter(description = "End date-time (ISO 8601 format)", example = "2026-02-25T23:59:59Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,
            @Parameter(description = "Maximum number of records to return", example = "100")
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(priceQueryService.getHistory(skuId, start, end, limit));
    }

    @Operation(
            summary = "Compare multiple SKUs",
            description = "Compare prices across multiple SKU locations with optional filtering and sorting. " +
                    "Returns price comparison metrics including cheapest, most expensive, best value (cheapest in-stock), " +
                    "price spread, and percentage difference."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SKU comparison completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or insufficient valid SKUs for comparison",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/compare")
    public ResponseEntity<SkuComparisonResponse> compareSkus(
            @Parameter(
                    description = "Comma-separated list of numeric SKU location IDs to compare (minimum 2 required)",
                    example = "1,2,3,4",
                    required = true
            )
            @RequestParam String skuIds,

            @Parameter(
                    description = "Filter to include only in-stock items. If true and less than 2 in-stock items remain, returns 400.",
                    example = "true"
            )
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,

            @Parameter(
                    description = "Sort order for results. Options: 'price' (ascending), 'price_desc' (descending), 'latest' (by capture time, newest first)",
                    example = "price"
            )
            @RequestParam(required = false, defaultValue = "price") String sortBy
    ) {
        // Parse comma-separated SKU IDs with safety
        List<Long> skuIdList;
        try {
            skuIdList = Arrays.stream(skuIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid SKU ID format in skuIds parameter");
        }

        // Validate minimum count
        if (skuIdList.size() < 2) {
            throw new IllegalArgumentException("At least 2 SKU IDs required");
        }

        return ResponseEntity.ok(priceQueryService.compareSkus(skuIdList, inStockOnly, sortBy));
    }
}
