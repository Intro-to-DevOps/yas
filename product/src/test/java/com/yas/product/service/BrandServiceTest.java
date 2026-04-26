package com.yas.product.service;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Product;
import com.yas.product.repository.BrandRepository;
import com.yas.product.viewmodel.brand.BrandListGetVm;
import com.yas.product.viewmodel.brand.BrandPostVm;
import com.yas.product.viewmodel.brand.BrandVm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    void test_retrieve_paginated_brands_successfully() {
        List<Brand> brands = List.of(new Brand(), new Brand());
        Page<Brand> brandPage = new PageImpl<>(brands);
        when(brandRepository.findAll(any(Pageable.class))).thenReturn(brandPage);

        BrandListGetVm result = brandService.getBrands(0, 2);

        assertEquals(2, result.brandContent().size());
        assertEquals(0, result.pageNo());
        assertEquals(2, result.pageSize());
    }

    @Test
    void test_create_brand_successfully() {
        BrandPostVm brandPostVm = new BrandPostVm("BrandName", "brand-slug", true);
        Brand brand = brandPostVm.toModel();
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        Brand result = brandService.create(brandPostVm);

        assertEquals("BrandName", result.getName());
        assertEquals("brand-slug", result.getSlug());
    }

    @Test
    void test_update_brand_successfully() {
        BrandPostVm brandPostVm = new BrandPostVm("UpdatedName", "updated-slug", true);
        Brand existingBrand = new Brand();
        existingBrand.setId(1L);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(existingBrand));
        when(brandRepository.save(any(Brand.class))).thenReturn(existingBrand);

        Brand result = brandService.update(brandPostVm, 1L);

        assertEquals("UpdatedName", result.getName());
        assertEquals("updated-slug", result.getSlug());
    }

    @Test
    void test_create_brand_with_existing_name() {
        BrandPostVm brandPostVm = new BrandPostVm("ExistingName", "existing-slug", true);
        when(brandRepository.findExistedName("ExistingName", null)).thenReturn(new Brand());

        Assertions.assertThrows(DuplicatedException.class, () -> {
            brandService.create(brandPostVm);
        });
    }

    @Test
    void test_update_brand_with_existing_name() {
        BrandPostVm brandPostVm = new BrandPostVm("ExistingName", "existing-slug", true);
        when(brandRepository.findExistedName("ExistingName", 1L)).thenReturn(new Brand());

        Assertions.assertThrows(DuplicatedException.class, () -> {
            brandService.update(brandPostVm, 1L);
        });
    }

    @Test
    void test_update_nonexistent_brand() {
        BrandPostVm brandPostVm = new BrandPostVm("NonExistentName", "nonexistent-slug", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> {
            brandService.update(brandPostVm, 1L);
        });
    }

    @Test
    void getBrandsByIds_whenIdsProvided_shouldReturnBrandVmList() {
        Brand brand1 = new Brand();
        brand1.setId(1L);
        brand1.setName("Brand1");
        brand1.setSlug("brand1");

        Brand brand2 = new Brand();
        brand2.setId(2L);
        brand2.setName("Brand2");
        brand2.setSlug("brand2");

        when(brandRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(brand1, brand2));

        List<BrandVm> result = brandService.getBrandsByIds(List.of(1L, 2L));

        assertEquals(2, result.size());
        assertEquals("Brand1", result.get(0).name());
        assertEquals("Brand2", result.get(1).name());
    }

    @Test
    void delete_whenBrandNotFound_shouldThrowNotFoundException() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> brandService.delete(99L));
    }

    @Test
    void delete_whenBrandHasProducts_shouldThrowBadRequestException() {
        Brand brand = new Brand();
        brand.setId(1L);
        Product product = new Product();
        brand.setProducts(List.of(product));

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        Assertions.assertThrows(BadRequestException.class, () -> brandService.delete(1L));
    }

    @Test
    void delete_whenBrandHasNoProducts_shouldDeleteSuccessfully() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setProducts(List.of());

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        brandService.delete(1L);

        verify(brandRepository).deleteById(1L);
    }
}