package com.yas.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private AbstractCircuitBreakFallbackHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AbstractCircuitBreakFallbackHandler() {};
    }

    @Test
    @DisplayName("handleBodilessFallback should throw the same exception")
    void handleBodilessFallback_shouldThrowSameException() {
        Throwable throwable = new RuntimeException("Test exception");

        assertThatThrownBy(() -> handler.handleBodilessFallback(throwable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");
    }

    @Test
    @DisplayName("handleTypedFallback should throw the same exception")
    void handleTypedFallback_shouldThrowSameException() {
        Throwable throwable = new RuntimeException("Test exception");

        assertThatThrownBy(() -> handler.handleTypedFallback(throwable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");
    }
}
