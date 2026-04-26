package com.yas.product.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceValidatorTest {

    private PriceValidator priceValidator;

    @BeforeEach
    void setUp() {
        priceValidator = new PriceValidator();
        priceValidator.initialize(null);
    }

    @Test
    void isValid_whenPriceIsPositive_shouldReturnTrue() {
        assertTrue(priceValidator.isValid(99.99, null));
    }

    @Test
    void isValid_whenPriceIsZero_shouldReturnTrue() {
        assertTrue(priceValidator.isValid(0.0, null));
    }

    @Test
    void isValid_whenPriceIsNegative_shouldReturnFalse() {
        assertFalse(priceValidator.isValid(-1.0, null));
    }

    @Test
    void isValid_whenPriceIsLarge_shouldReturnTrue() {
        assertTrue(priceValidator.isValid(1_000_000.0, null));
    }
}
