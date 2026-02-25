package io.priceintel.dto;

import io.priceintel.enums.Availability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuComparisonItem {

    private Long skuId;
    private BigDecimal price;
    private Availability availability;
    private Instant capturedAt;
    private Integer rank;
}
