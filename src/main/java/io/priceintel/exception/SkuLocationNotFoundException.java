package io.priceintel.exception;

public class SkuLocationNotFoundException extends RuntimeException {

    public SkuLocationNotFoundException(Long id) {
        super("SkuLocation with id " + id + " does not exist");
    }
}

