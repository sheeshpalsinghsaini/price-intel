package io.priceintel.repository;

import io.priceintel.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByBrandNameAndProductNameAndPackSize(
            String brandName,
            String productName,
            String packSize
    );

    Optional<Product> findByBrandNameAndProductNameAndPackSize(
            String brandName,
            String productName,
            String packSize
    );
}

