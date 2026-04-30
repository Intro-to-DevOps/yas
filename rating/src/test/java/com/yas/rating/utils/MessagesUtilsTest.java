package com.yas.rating.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_withExistingKey_returnFormattedMessage() {
        String message = MessagesUtils.getMessage("RATING_NOT_FOUND", 1L);
        assertThat(message).isEqualTo("RATING 1 is not found");
    }

    @Test
    void getMessage_withNonExistingKey_returnKeyItself() {
        String message = MessagesUtils.getMessage("NON_EXISTING_KEY", "arg1");
        assertThat(message).isEqualTo("NON_EXISTING_KEY");
    }

    @Test
    void testConstructor() {
        MessagesUtils instance = new MessagesUtils();
        assertThat(instance).isNotNull();
    }
}
