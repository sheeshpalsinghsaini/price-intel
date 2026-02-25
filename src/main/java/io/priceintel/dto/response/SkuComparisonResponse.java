package io.priceintel.dto.response;

import io.priceintel.dto.SkuComparisonItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuComparisonResponse {

    private Integer totalCompared;
    private Long cheapestSkuId;
    private Long mostExpensiveSkuId;
    private Long bestValueSkuId;
    private BigDecimal priceSpread;
    private BigDecimal percentageDifference;
    private List<SkuComparisonItem> results;
}