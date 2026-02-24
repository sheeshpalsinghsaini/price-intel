package io.priceintel.exception;

public class PlatformNotFoundException extends RuntimeException {

    public PlatformNotFoundException(Long id) {
        super("Platform with id " + id + " does not exist");
    }
}

