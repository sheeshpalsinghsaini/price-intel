package io.priceintel.exception;

public class PriceSnapshotNotFoundException extends RuntimeException {

    public PriceSnapshotNotFoundException(Long skuId) {
        super("Price snapshot not found for SKU location with id " + skuId);
    }
}

