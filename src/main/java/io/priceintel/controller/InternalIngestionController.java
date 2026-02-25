package io.priceintel.controller;

import io.priceintel.crawler.dto.IngestionRequest;
import io.priceintel.crawler.facade.IngestionFacadeService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalIngestionController {

    private final IngestionFacadeService ingestionFacadeService;

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingest(@RequestBody IngestionRequest request) {
        if (request == null) {
            log.warn("Received null ingestion request body");
            throw new IllegalArgumentException("Request body cannot be null");
        }

        log.info("Ingestion request received: brand={}, product={}, platform={}, city={}",
                request.getBrandName(), request.getProductName(),
                request.getPlatformName(), request.getCity());

        ingestionFacadeService.ingest(request);

        return ResponseEntity.ok(Map.of("message", "Ingestion successful"));
    }
}
