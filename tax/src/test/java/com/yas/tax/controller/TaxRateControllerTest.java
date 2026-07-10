package com.yas.tax.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxRate;
import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TaxRateController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class TaxRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaxRateService taxRateService;

    @Test
    void getPageableTaxRates_shouldReturnPage() throws Exception {
        TaxRateListGetVm response = new TaxRateListGetVm(
            List.of(new TaxRateGetDetailVm(1L, 10.0, "10000", "VAT", "HCM", "VN")),
            0, 10, 1, 1, true
        );
        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(response);

        mockMvc.perform(get("/backoffice/tax-rates/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].id").value(1))
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].rate").value(10.0))
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].zipCode").value("10000"))
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].taxClassName").value("VAT"))
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].stateOrProvinceName").value("HCM"))
            .andExpect(jsonPath("$.taxRateGetDetailContent[0].countryName").value("VN"));
    }

    @Test
    void getTaxRate_whenExists_shouldReturnVm() throws Exception {
        TaxRateVm response = new TaxRateVm(1L, 10.0, "10000", 1L, 2L, 3L);
        when(taxRateService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/backoffice/tax-rates/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rate").value(10.0))
            .andExpect(jsonPath("$.zipCode").value("10000"))
            .andExpect(jsonPath("$.taxClassId").value(1))
            .andExpect(jsonPath("$.stateOrProvinceId").value(2))
            .andExpect(jsonPath("$.countryId").value(3));
    }

    @Test
    void getTaxRate_whenNotExists_shouldReturnNotFound() throws Exception {
        when(taxRateService.findById(1L)).thenThrow(new NotFoundException("Tax rate not found"));

        mockMvc.perform(get("/backoffice/tax-rates/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createTaxRate_whenValid_shouldReturnCreated() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 1L, 2L, 3L);
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder().id(1L).rate(10.0).zipCode("10000").taxClass(taxClass).stateOrProvinceId(2L).countryId(3L).build();
        
        when(taxRateService.createTaxRate(any(TaxRatePostVm.class))).thenReturn(taxRate);

        mockMvc.perform(post("/backoffice/tax-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rate").value(10.0));
    }

    @Test
    void createTaxRate_whenTaxClassMissing_shouldReturnNotFound() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 1L, 2L, 3L);
        when(taxRateService.createTaxRate(any(TaxRatePostVm.class))).thenThrow(new NotFoundException("Tax class not found"));

        mockMvc.perform(post("/backoffice/tax-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateTaxRate_whenValid_shouldReturnNoContent() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(12.0, "12000", 1L, 2L, 3L);
        doNothing().when(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-rates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateTaxRate_whenTaxRateMissing_shouldReturnNotFound() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(12.0, "12000", 1L, 2L, 3L);
        doThrow(new NotFoundException("Tax rate not found")).when(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-rates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteTaxRate_shouldReturnNoContent() throws Exception {
        doNothing().when(taxRateService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-rates/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxRate_whenNotExists_shouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Tax rate not found")).when(taxRateService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-rates/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getTaxPercentByAddress_shouldReturnDouble() throws Exception {
        when(taxRateService.getTaxPercent(1L, 3L, 2L, "10000")).thenReturn(8.5);

        mockMvc.perform(get("/backoffice/tax-rates/tax-percent")
                .param("taxClassId", "1")
                .param("countryId", "3")
                .param("stateOrProvinceId", "2")
                .param("zipCode", "10000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(8.5));
    }

    @Test
    void getBatchTaxPercentsByAddress_shouldReturnList() throws Exception {
        TaxRateVm response = new TaxRateVm(1L, 8.5, "10000", 1L, 2L, 3L);
        when(taxRateService.getBulkTaxRate(List.of(1L), 3L, 2L, "10000")).thenReturn(List.of(response));

        mockMvc.perform(get("/backoffice/tax-rates/location-based-batch")
                .param("taxClassIds", "1")
                .param("countryId", "3")
                .param("stateOrProvinceId", "2")
                .param("zipCode", "10000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].rate").value(8.5));
    }
}
