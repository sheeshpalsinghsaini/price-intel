package io.priceintel.dto.response;

import io.priceintel.dto.PricePoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryResponse {

    private Long skuId;
    private Integer count;
    private List<PricePoint> history;
}

