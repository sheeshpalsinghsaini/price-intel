package io.priceintel.crawler.dto;

import io.priceintel.enums.Availability;
import io.priceintel.enums.CrawlStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRequest {

    private String brandName;
    private String productName;
    private String packSize;
    private String platformName;
    private String city;
    private String productUrl;
    private BigDecimal sellingPrice;
    private BigDecimal discount;
    private Availability availability;
    private CrawlStatus crawlStatus;
    private Instant capturedAt;
}

