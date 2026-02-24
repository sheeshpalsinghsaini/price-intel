package io.priceintel.repository;

import io.priceintel.entity.Platform;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRepository extends JpaRepository<Platform, Long> {
    Optional<Platform> findByName(String name);

    boolean existsByName(String name);

    Optional<Platform> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
