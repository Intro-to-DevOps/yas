package com.yas.product.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProductConverterTest {

    @Test
    void toSlug_withSimpleString_shouldReturnLowercaseSlug() {
        assertEquals("hello-world", ProductConverter.toSlug("Hello World"));
    }

    @Test
    void toSlug_withSpecialCharacters_shouldReplaceWithDash() {
        assertEquals("hello-world", ProductConverter.toSlug("Hello & World"));
    }

    @Test
    void toSlug_withMultipleSpaces_shouldCollapseDashes() {
        assertEquals("hello-world", ProductConverter.toSlug("Hello   World"));
    }

    @Test
    void toSlug_withLeadingDash_shouldRemoveLeadingDash() {
        assertEquals("hello", ProductConverter.toSlug("-hello"));
    }

    @Test
    void toSlug_withLeadingSpecialChar_shouldRemoveLeadingDash() {
        assertEquals("hello", ProductConverter.toSlug("!hello"));
    }

    @Test
    void toSlug_withNumbersAndLetters_shouldPreserveAlphanumeric() {
        assertEquals("product-123", ProductConverter.toSlug("Product 123"));
    }

    @Test
    void toSlug_withHyphens_shouldPreserveHyphens() {
        assertEquals("my-product", ProductConverter.toSlug("my-product"));
    }

    @Test
    void toSlug_withLeadingAndTrailingSpaces_shouldTrimAndConvert() {
        assertEquals("hello", ProductConverter.toSlug("  hello  "));
    }
}
