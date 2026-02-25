package io.priceintel.controller;

import io.priceintel.dto.response.SkuComparisonResponse;
import io.priceintel.enums.ComparisonSortType;
import io.priceintel.service.PriceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Tag(name = "Product Comparison", description = "APIs for comparing prices across platforms for a product")
public class ProductComparisonController {

    private final PriceQueryService priceQueryService;

    @Operation(
            summary = "Compare product prices across platforms",
            description = "Compare prices for a specific product across all available platforms. " +
                    "Optionally filter by city and/or in-stock availability. " +
                    "Supports pagination for large result sets. " +
                    "Returns price comparison metrics including cheapest, most expensive, best value, " +
                    "price spread, and percentage difference across all SKU locations for the product."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product price comparison completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product ID or insufficient SKU locations found",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Product not found or no active SKU locations available",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{productId}/compare")
    public ResponseEntity<SkuComparisonResponse> compareProduct(
            @Parameter(
                    description = "Product ID to compare prices for",
                    example = "1",
                    required = true
            )
            @PathVariable Long productId,

            @Parameter(
                    description = "Optional city filter to compare prices only within a specific city (case-insensitive)",
                    example = "Bangalore"
            )
            @RequestParam(required = false) String city,

            @Parameter(
                    description = "Filter to include only in-stock items. If true and less than 2 in-stock items remain, returns 400.",
                    example = "false"
            )
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,

            @Parameter(
                    description = "Sort order for results. Options: PRICE_ASC, PRICE_DESC, LATEST",
                    example = "PRICE_ASC"
            )
            @RequestParam(required = false, defaultValue = "PRICE_ASC") ComparisonSortType sortType,

            @Parameter(
                    description = "Page number for pagination (0-based). Optional.",
                    example = "0"
            )
            @RequestParam(required = false) Integer page,

            @Parameter(
                    description = "Page size for pagination (max 100). Optional.",
                    example = "20"
            )
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(priceQueryService.compareProduct(productId, city, inStockOnly, sortType, page, size));
    }
}

