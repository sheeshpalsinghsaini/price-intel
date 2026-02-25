package io.priceintel.dto.response;

import io.priceintel.enums.Availability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePoint {

    private BigDecimal sellingPrice;
    private BigDecimal discount;
    private Availability availability;
    private Instant capturedAt;
}

