package io.priceintel.crawler.scheduler;

import io.priceintel.crawler.job.BlinkitSimulatedCrawlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private final BlinkitSimulatedCrawlJob blinkitSimulatedCrawlJob;

    @Scheduled(fixedDelay = 60000)
    public void runBlinkitJob() {
        log.info("Scheduler triggered: Blinkit crawl job");

        long start = System.currentTimeMillis();
        boolean success = true;

        try {
            blinkitSimulatedCrawlJob.execute();
        } catch (Exception e) {
            success = false;
            log.error("Error executing Blinkit crawl job: {}", e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (success) {
                log.info("Blinkit crawl job completed in {} ms", duration);
            } else {
                log.info("Blinkit crawl job failed after {} ms", duration);
            }
        }
    }
}

