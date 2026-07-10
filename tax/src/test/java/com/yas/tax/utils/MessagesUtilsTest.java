package com.yas.tax.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void instantiateMessagesUtils_shouldSucceed() {
        MessagesUtils utils = new MessagesUtils();
        assertNotNull(utils);
    }

    @Test
    void getMessage_whenKeyMissing_shouldReturnKey() {
        String result = MessagesUtils.getMessage("NON_EXISTING_KEY");
        assertEquals("NON_EXISTING_KEY", result);
    }

    @Test
    void getMessage_withParameters_shouldFormatMessage() {
        String result = MessagesUtils.getMessage("Hello {}!", "World");
        assertEquals("Hello World!", result);
    }
}
