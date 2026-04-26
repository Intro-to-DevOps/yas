package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    @Test
    void findAllTaxClasses_shouldReturnSortedMappedList() {
        TaxClass vat = TaxClass.builder().id(1L).name("VAT").build();
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))).thenReturn(List.of(vat));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).hasSize(1);
        assertEquals("VAT", result.getFirst().name());
    }

    @Test
    void findById_whenNotFound_shouldThrowNotFoundException() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.findById(1L));
    }

    @Test
    void findById_whenFound_shouldReturnVm() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

        TaxClassVm result = taxClassService.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("VAT", result.name());
    }

    @Test
    void create_whenDuplicateName_shouldThrowDuplicatedException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.create(postVm));
    }

    @Test
    void create_whenValid_shouldSaveAndReturnEntity() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenAnswer(invocation -> {
            TaxClass saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TaxClass result = taxClassService.create(postVm);

        assertEquals(1L, result.getId());
        assertEquals("VAT", result.getName());
    }

    @Test
    void update_whenTaxClassMissing_shouldThrowNotFoundException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.update(postVm, 1L));
    }

    @Test
    void update_whenNameUsedByOther_shouldThrowDuplicatedException() {
        TaxClass existing = TaxClass.builder().id(1L).name("OLD").build();
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("VAT", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.update(postVm, 1L));
    }

    @Test
    void update_whenValid_shouldUpdateNameAndSave() {
        TaxClass existing = TaxClass.builder().id(1L).name("OLD").build();
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("VAT", 1L)).thenReturn(false);

        taxClassService.update(postVm, 1L);

        assertEquals("VAT", existing.getName());
        verify(taxClassRepository).save(existing);
    }

    @Test
    void delete_whenTaxClassMissing_shouldThrowNotFoundException() {
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxClassService.delete(1L));
    }

    @Test
    void delete_whenTaxClassExists_shouldDeleteById() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void getPageableTaxClasses_shouldReturnPageMetadataAndContent() {
        TaxClass vat = TaxClass.builder().id(1L).name("VAT").build();
        when(taxClassRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(vat), PageRequest.of(0, 10), 1));

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result.taxClassContent()).hasSize(1);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
    }
}
