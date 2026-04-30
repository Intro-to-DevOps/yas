package com.yas.rating.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void testConstants() {
        assertThat(Constants.ErrorCode.RATING_NOT_FOUND).isEqualTo("RATING_NOT_FOUND");
        assertThat(Constants.ErrorCode.PRODUCT_NOT_FOUND).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(Constants.ErrorCode.CUSTOMER_NOT_FOUND).isEqualTo("CUSTOMER_NOT_FOUND");
        assertThat(Constants.ErrorCode.RESOURCE_ALREADY_EXISTED).isEqualTo("RESOURCE_ALREADY_EXISTED");
        assertThat(Constants.ErrorCode.ACCESS_DENIED).isEqualTo("ACCESS_DENIED");
        assertThat(Constants.Message.SUCCESS_MESSAGE).isEqualTo("SUCCESS");
    }

    @Test
    void testConstantsConstructor() throws Exception {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Constants instance = constructor.newInstance();
        assertThat(instance).isNotNull();

        Constructor<Constants.ErrorCode> errorCodeConstructor = Constants.ErrorCode.class.getDeclaredConstructor(Constants.class);
        errorCodeConstructor.setAccessible(true);
        Constants.ErrorCode errorCode = errorCodeConstructor.newInstance(instance);
        assertThat(errorCode).isNotNull();

        Constructor<Constants.Message> messageConstructor = Constants.Message.class.getDeclaredConstructor(Constants.class);
        messageConstructor.setAccessible(true);
        Constants.Message message = messageConstructor.newInstance(instance);
        assertThat(message).isNotNull();
    }
}
