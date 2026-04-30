package com.yas.payment.service.provider.handler;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaypalHandlerTest {

    private PaypalService paypalService;
    private PaymentProviderService paymentProviderService;
    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        paypalService = Mockito.mock(PaypalService.class);
        paymentProviderService = Mockito.mock(PaymentProviderService.class);
        paypalHandler = new PaypalHandler(paymentProviderService, paypalService);
    }

    @Test
    void getProviderId_shouldReturnPaypal() {
        assertEquals(PaymentMethod.PAYPAL.name(), paypalHandler.getProviderId());
    }

    @Test
    void initPayment_shouldReturnInitiatedPayment() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .totalPrice(BigDecimal.TEN)
                .checkoutId("checkout-1")
                .build();

        PaypalCreatePaymentResponse response = PaypalCreatePaymentResponse.builder()
                .status("CREATED")
                .paymentId("pay-1")
                .redirectUrl("http://redirect")
                .build();

        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class))).thenReturn(response);

        InitiatedPayment result = paypalHandler.initPayment(requestVm);

        assertNotNull(result);
        assertEquals("CREATED", result.getStatus());
        assertEquals("pay-1", result.getPaymentId());
        assertEquals("http://redirect", result.getRedirectUrl());
    }

    @Test
    void capturePayment_shouldReturnCapturedPayment() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
                .token("token-1")
                .build();

        PaypalCapturePaymentResponse response = PaypalCapturePaymentResponse.builder()
                .checkoutId("checkout-1")
                .amount(BigDecimal.TEN)
                .paymentFee(BigDecimal.ONE)
                .gatewayTransactionId("trans-1")
                .paymentMethod("PAYPAL")
                .paymentStatus("COMPLETED")
                .build();

        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class))).thenReturn(response);

        CapturedPayment result = paypalHandler.capturePayment(requestVm);

        assertNotNull(result);
        assertEquals("checkout-1", result.getCheckoutId());
        assertEquals(BigDecimal.TEN, result.getAmount());
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
    }
}
