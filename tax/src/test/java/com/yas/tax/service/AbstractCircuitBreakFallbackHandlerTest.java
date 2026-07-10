package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private static class TestCircuitBreakFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        // Concrete test subclass
    }

    private final TestCircuitBreakFallbackHandler handler = new TestCircuitBreakFallbackHandler();

    @Test
    void handleBodilessFallback_shouldThrowException() {
        Exception testException = new Exception("Test exception");
        assertThrows(Exception.class, () -> handler.handleBodilessFallback(testException));
    }

    @Test
    void handleTypedFallback_shouldThrowException() {
        Exception testException = new Exception("Test typed exception");
        assertThrows(Exception.class, () -> handler.handleTypedFallback(testException));
    }
}
