package io.priceintel.service;

import io.priceintel.entity.Product;
import io.priceintel.repository.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(String brandName, String productName, String packSize) {
        log.debug("Creating product: brandName={}, productName={}, packSize={}", brandName, productName, packSize);

        Optional<Product> existingProduct = productRepository
                .findByBrandNameAndProductNameAndPackSize(brandName, productName, packSize);

        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();
            log.info("Returning existing product with id={}", product.getId());
            return product;
        }

        Product product = Product.builder()
                .brandName(brandName)
                .productName(productName)
                .packSize(packSize)
                .createdAt(Instant.now())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created new product with id={} brandName={} productName={}",
                savedProduct.getId(), savedProduct.getBrandName(), savedProduct.getProductName());
        return savedProduct;
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}

