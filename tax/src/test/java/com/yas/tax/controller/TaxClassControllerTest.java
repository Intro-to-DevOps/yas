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
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TaxClassController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class TaxClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaxClassService taxClassService;

    @Test
    void getPageableTaxClasses_shouldReturnPage() throws Exception {
        TaxClassListGetVm response = new TaxClassListGetVm(
            List.of(new TaxClassVm(1L, "VAT")),
            0, 10, 1, 1, true
        );
        when(taxClassService.getPageableTaxClasses(0, 10)).thenReturn(response);

        mockMvc.perform(get("/backoffice/tax-classes/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taxClassContent[0].id").value(1))
            .andExpect(jsonPath("$.taxClassContent[0].name").value("VAT"));
    }

    @Test
    void listTaxClasses_shouldReturnList() throws Exception {
        List<TaxClassVm> response = List.of(new TaxClassVm(1L, "VAT"));
        when(taxClassService.findAllTaxClasses()).thenReturn(response);

        mockMvc.perform(get("/backoffice/tax-classes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("VAT"));
    }

    @Test
    void getTaxClass_whenExists_shouldReturnVm() throws Exception {
        TaxClassVm response = new TaxClassVm(1L, "VAT");
        when(taxClassService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/backoffice/tax-classes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("VAT"));
    }

    @Test
    void getTaxClass_whenNotExists_shouldThrowNotFoundException() throws Exception {
        when(taxClassService.findById(1L)).thenThrow(new NotFoundException("Tax class not found"));

        mockMvc.perform(get("/backoffice/tax-classes/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createTaxClass_whenValid_shouldReturnCreated() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        when(taxClassService.create(any(TaxClassPostVm.class))).thenReturn(taxClass);

        mockMvc.perform(post("/backoffice/tax-classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("VAT"));
    }

    @Test
    void createTaxClass_whenDuplicate_shouldReturnBadRequest() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassService.create(any(TaxClassPostVm.class))).thenThrow(new DuplicatedException("Name already exists"));

        mockMvc.perform(post("/backoffice/tax-classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createTaxClass_whenInvalid_shouldReturnBadRequest() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", ""); // Name cannot be empty or blank
        
        mockMvc.perform(post("/backoffice/tax-classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateTaxClass_whenValid_shouldReturnNoContent() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT-Updated");
        doNothing().when(taxClassService).update(any(TaxClassPostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-classes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateTaxClass_whenDuplicate_shouldReturnBadRequest() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT-Duplicate");
        doThrow(new DuplicatedException("Name already exists")).when(taxClassService).update(any(TaxClassPostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-classes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTaxClass_shouldReturnNoContent() throws Exception {
        doNothing().when(taxClassService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-classes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxClass_whenNotExists_shouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Tax class not found")).when(taxClassService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-classes/1"))
            .andExpect(status().isNotFound());
    }
}
