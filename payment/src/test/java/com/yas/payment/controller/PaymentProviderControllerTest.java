package com.yas.payment.controller;

import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentProviderController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentProviderService paymentProviderService;

    @Test
    void create_shouldReturnCreatedStatus() throws Exception {
        PaymentProviderVm responseVm = new PaymentProviderVm("test", "Test Provider", "http://config", 1, null, null);

        when(paymentProviderService.create(any(CreatePaymentVm.class))).thenReturn(responseVm);

        mockMvc.perform(post("/backoffice/payment-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"test\", \"name\":\"Test Provider\", \"configureUrl\":\"http://config\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test"))
                .andExpect(jsonPath("$.name").value("Test Provider"));
    }

    @Test
    void update_shouldReturnOkStatus() throws Exception {
        PaymentProviderVm responseVm = new PaymentProviderVm("test", "Updated Provider", "http://config", 2, null, null);

        when(paymentProviderService.update(any(UpdatePaymentVm.class))).thenReturn(responseVm);

        mockMvc.perform(put("/backoffice/payment-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"test\", \"name\":\"Updated Provider\", \"configureUrl\":\"http://config\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test"))
                .andExpect(jsonPath("$.name").value("Updated Provider"));
    }

    @Test
    void getAll_shouldReturnListOfPaymentProviders() throws Exception {
        PaymentProviderVm responseVm = new PaymentProviderVm("test", "Test Provider", "http://config", 1, null, null);

        when(paymentProviderService.getEnabledPaymentProviders(any(Pageable.class))).thenReturn(List.of(responseVm));

        mockMvc.perform(get("/storefront/payment-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test"))
                .andExpect(jsonPath("$[0].name").value("Test Provider"));
    }
}
