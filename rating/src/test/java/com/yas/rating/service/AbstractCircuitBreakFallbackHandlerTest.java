package com.yas.rating.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private static class TestHandler extends AbstractCircuitBreakFallbackHandler {
    }

    private final TestHandler handler = new TestHandler();

    @Test
    void handleBodilessFallback_shouldThrowOriginalException() {
        Throwable cause = new RuntimeException("Test exception");
        assertThatThrownBy(() -> handler.handleBodilessFallback(cause))
                .isSameAs(cause);
    }

    @Test
    void handleFallback_shouldThrowOriginalException() {
        Throwable cause = new RuntimeException("Test exception");
        assertThatThrownBy(() -> handler.handleFallback(cause))
                .isSameAs(cause);
    }
}
