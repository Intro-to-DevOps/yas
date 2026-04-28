package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private LocationService locationService;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxRateService taxRateService;

    @Test
    void createTaxRate_whenTaxClassMissing_shouldThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 1L, 2L, 3L);
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.createTaxRate(postVm));
    }

    @Test
    void createTaxRate_whenValid_shouldSaveTaxRate() {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 1L, 2L, 3L);
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();

        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxRate result = taxRateService.createTaxRate(postVm);

        assertEquals(10.0, result.getRate());
        assertEquals("10000", result.getZipCode());
        assertEquals(1L, result.getTaxClass().getId());
        assertEquals(2L, result.getStateOrProvinceId());
        assertEquals(3L, result.getCountryId());
    }

    @Test
    void updateTaxRate_whenTaxRateMissing_shouldThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(8.0, null, 1L, null, 3L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(postVm, 10L));
    }

    @Test
    void updateTaxRate_whenTaxClassMissing_shouldThrowNotFoundException() {
        TaxRate taxRate = TaxRate.builder().id(10L).build();
        TaxRatePostVm postVm = new TaxRatePostVm(8.0, null, 1L, null, 3L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(postVm, 10L));
    }

    @Test
    void updateTaxRate_whenValid_shouldUpdateAndSave() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder().id(10L).rate(2.0).zipCode("old").build();
        TaxRatePostVm postVm = new TaxRatePostVm(8.0, "20000", 1L, 20L, 30L);

        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);

        taxRateService.updateTaxRate(postVm, 10L);

        assertEquals(8.0, taxRate.getRate());
        assertEquals("20000", taxRate.getZipCode());
        assertEquals(20L, taxRate.getStateOrProvinceId());
        assertEquals(30L, taxRate.getCountryId());
        verify(taxRateRepository).save(taxRate);
    }

    @Test
    void delete_whenTaxRateMissing_shouldThrowNotFoundException() {
        when(taxRateRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.delete(99L));
    }

    @Test
    void delete_whenTaxRateExists_shouldDelete() {
        when(taxRateRepository.existsById(99L)).thenReturn(true);

        taxRateService.delete(99L);

        verify(taxRateRepository).deleteById(99L);
    }

    @Test
    void findById_whenFound_shouldReturnVm() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder().id(10L).rate(8.5).zipCode("10000").taxClass(taxClass).countryId(3L).build();
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(10L);

        assertEquals(10L, result.id());
        assertEquals(1L, result.taxClassId());
    }

    @Test
    void getPageableTaxRates_whenStateListEmpty_shouldReturnEmptyDetailList() {
        TaxRate taxRate = TaxRate.builder().id(10L).stateOrProvinceId(2L).build();
        when(taxRateRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxRate), PageRequest.of(0, 10), 1));
        when(locationService.getStateOrProvinceAndCountryNames(any())).thenReturn(List.of());

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).isEmpty();
    }

    @Test
    void getPageableTaxRates_whenLocationDataPresent_shouldBuildDetailRows() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder().id(10L).rate(8.5).zipCode("10000").taxClass(taxClass)
            .stateOrProvinceId(2L).countryId(3L).build();

        when(taxRateRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxRate), PageRequest.of(0, 10), 1));
        when(locationService.getStateOrProvinceAndCountryNames(List.of(2L)))
            .thenReturn(List.of(new StateOrProvinceAndCountryGetNameVm(2L, "HCM", "VN")));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).hasSize(1);
        assertEquals("VAT", result.taxRateGetDetailContent().getFirst().taxClassName());
        assertEquals("HCM", result.taxRateGetDetailContent().getFirst().stateOrProvinceName());
    }

    @Test
    void getTaxPercent_whenRepositoryReturnsNull_shouldReturnZero() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "10000", 3L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(3L, 1L, 2L, "10000");

        assertEquals(0.0, result);
    }

    @Test
    void getTaxPercent_whenRepositoryHasValue_shouldReturnValue() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "10000", 3L)).thenReturn(7.0);

        double result = taxRateService.getTaxPercent(3L, 1L, 2L, "10000");

        assertEquals(7.0, result);
    }

    @Test
    void getBulkTaxRate_shouldQueryByHashSetAndMapToVm() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder().id(10L).rate(5.0).zipCode("10000").taxClass(taxClass)
            .stateOrProvinceId(2L).countryId(3L).build();
        when(taxRateRepository.getBatchTaxRates(eq(3L), eq(2L), eq("10000"), eq(Set.of(1L))))
            .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 3L, 2L, "10000");

        assertThat(result).hasSize(1);
        assertEquals(10L, result.getFirst().id());
        verify(locationService, never()).getStateOrProvinceAndCountryNames(any());
    }
}