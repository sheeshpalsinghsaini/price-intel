package io.priceintel.crawler.job;

import io.priceintel.crawler.dto.IngestionRequest;
import io.priceintel.crawler.facade.IngestionFacadeService;
import io.priceintel.enums.Availability;
import io.priceintel.enums.CrawlStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlinkitSimulatedCrawlJob {

    private final IngestionFacadeService ingestionFacadeService;
    private final Random random = new Random();

    public void execute() {
        log.info("Starting Blinkit simulated crawl job");

        BigDecimal sellingPrice = generateRandomPrice(240, 260);
        BigDecimal discount = generateRandomDiscount(0, 20);
        Availability availability = generateRandomAvailability();

        IngestionRequest request = IngestionRequest.builder()
                .brandName("Amul")
                .productName("Butter")
                .packSize("500g")
                .platformName("Blinkit")
                .city("Bangalore")
                .productUrl("https://blinkit.com/amul-butter")
                .sellingPrice(sellingPrice)
                .discount(discount)
                .availability(availability)
                .crawlStatus(CrawlStatus.SUCCESS)
                .capturedAt(Instant.now())
                .build();

        ingestionFacadeService.ingest(request);

        log.info("Completed Blinkit simulated crawl job: price={}, discount={}, availability={}",
                sellingPrice, discount, availability);
    }

    private BigDecimal generateRandomPrice(int min, int max) {
        double price = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(price)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal generateRandomDiscount(int min, int max) {
        double discount = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(discount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Availability generateRandomAvailability() {
        return random.nextBoolean() ? Availability.IN_STOCK : Availability.OUT_OF_STOCK;
    }
}

