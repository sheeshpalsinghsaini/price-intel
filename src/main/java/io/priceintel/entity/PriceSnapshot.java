package io.priceintel.entity;

import java.math.BigDecimal;
import java.time.Instant;

import io.priceintel.enums.Availability;
import io.priceintel.enums.CrawlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price_snapshots")
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_location_id", nullable = false)
    private SkuLocation skuLocation;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice;

    @Column(name="discount")
    private BigDecimal discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false)
    private Availability availability;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_status", nullable = false)
    private CrawlStatus crawlStatus;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

}
