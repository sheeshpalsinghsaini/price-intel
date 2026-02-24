package io.priceintel.service;

import io.priceintel.entity.Platform;
import io.priceintel.repository.PlatformRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformService {

    private final PlatformRepository platformRepository;

    public Platform createPlatform(String name) {
        log.debug("Creating platform: name={}", name);

        String normalizedName = normalizeName(name);

        Optional<Platform> existing = platformRepository.findByNameIgnoreCase(normalizedName);
        if (existing.isPresent()) {
            Platform platform = existing.get();
            log.info("Returning existing platform with id={} name={}", platform.getId(), platform.getName());
            return platform;
        }

        Platform platform = Platform.builder()
                .name(normalizedName)
                .createdAt(Instant.now())
                .build();

        Platform savedPlatform = platformRepository.save(platform);
        log.info("Created new platform with id={} name={}", savedPlatform.getId(), savedPlatform.getName());
        return savedPlatform;
    }

    public List<Platform> getAllPlatforms() {
        return platformRepository.findAll();
    }

    public Optional<Platform> getPlatformById(Long id) {
        return platformRepository.findById(id);
    }

    public Optional<Platform> getPlatformByName(String name) {
        return platformRepository.findByNameIgnoreCase(normalizeName(name));
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isBlank()) {
            log.warn("Invalid platform name: empty or null");
            throw new IllegalArgumentException("Platform name cannot be empty");
        }
        return name.trim();
    }
}

