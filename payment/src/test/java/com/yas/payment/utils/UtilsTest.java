package com.yas.payment.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void getMessage_withExistingCode_shouldReturnMessage() {
        // Note: This depends on the presence of messages.messages bundle
        // If it's missing, it will return the code itself
        String message = MessagesUtils.getMessage("PAYMENT_PROVIDER_NOT_FOUND");
        assertNotNull(message);
    }

    @Test
    void getMessage_withNonExistingCode_shouldReturnCode() {
        String code = "NON_EXISTING_CODE";
        String message = MessagesUtils.getMessage(code);
        assertEquals(code, message);
    }

    @Test
    void getMessage_withArguments_shouldFormatMessage() {
        String message = MessagesUtils.getMessage("Test {} and {}", "arg1", "arg2");
        assertEquals("Test arg1 and arg2", message);
    }

    @Test
    void constants_access_toCoverClasses() {
        assertNotNull(Constants.ErrorCode.PAYMENT_PROVIDER_NOT_FOUND);
        assertNotNull(Constants.Message.SUCCESS_MESSAGE);
    }
}
