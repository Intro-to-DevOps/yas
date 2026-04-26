package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductOption;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    private NoFileMediaVm noFileMediaVm;

    @BeforeEach
    void setUp() {
        noFileMediaVm = new NoFileMediaVm(1L, "caption", "file.jpg", "image/jpeg", "http://example.com/img.jpg");
    }

    @Test
    void getProductDetailById_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(99L));
    }

    @Test
    void getProductDetailById_whenProductNotPublished_shouldThrowNotFoundException() {
        Product product = buildProduct(1L, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(1L));
    }

    @Test
    void getProductDetailById_whenSimpleProduct_shouldReturnDetailInfoVm() {
        Product product = buildProduct(1L, true);
        product.setThumbnailMediaId(10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(10L)).thenReturn(noFileMediaVm);

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Product A", result.getName());
        assertNotNull(result.getThumbnail());
        assertEquals("http://example.com/img.jpg", result.getThumbnail().url());
    }

    @Test
    void getProductDetailById_whenNoBrand_shouldReturnNullBrandInfo() {
        Product product = buildProduct(1L, true);
        product.setBrand(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNull(result.getBrandId());
        assertNull(result.getBrandName());
    }

    @Test
    void getProductDetailById_whenHasBrand_shouldReturnBrandInfo() {
        Product product = buildProduct(1L, true);
        Brand brand = new Brand();
        brand.setId(5L);
        brand.setName("TestBrand");
        product.setBrand(brand);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertEquals(5L, result.getBrandId());
        assertEquals("TestBrand", result.getBrandName());
    }

    @Test
    void getProductDetailById_whenHasCategories_shouldReturnCategoryList() {
        Product product = buildProduct(1L, true);
        Category cat = new Category();
        cat.setId(3L);
        cat.setName("TestCategory");
        ProductCategory productCategory = ProductCategory.builder().category(cat).build();
        product.setProductCategories(List.of(productCategory));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertEquals(1, result.getCategories().size());
        assertEquals("TestCategory", result.getCategories().getFirst().getName());
    }

    @Test
    void getProductDetailById_whenHasImages_shouldReturnImageList() {
        Product product = buildProduct(1L, true);
        ProductImage image1 = ProductImage.builder().imageId(20L).build();
        ProductImage image2 = ProductImage.builder().imageId(21L).build();
        product.setProductImages(List.of(image1, image2));
        NoFileMediaVm img1Vm = new NoFileMediaVm(20L, "", "", "", "http://cdn/20.jpg");
        NoFileMediaVm img2Vm = new NoFileMediaVm(21L, "", "", "", "http://cdn/21.jpg");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(20L)).thenReturn(img1Vm);
        when(mediaService.getMedia(21L)).thenReturn(img2Vm);

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertEquals(2, result.getProductImages().size());
        assertThat(result.getProductImages()).extracting(img -> img.url())
            .containsExactly("http://cdn/20.jpg", "http://cdn/21.jpg");
    }

    @Test
    void getProductDetailById_whenNoThumbnail_shouldReturnNullThumbnail() {
        Product product = buildProduct(1L, true);
        product.setThumbnailMediaId(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNull(result.getThumbnail());
    }

    @Test
    void getProductDetailById_whenHasOptions_shouldReturnVariations() {
        Product product = buildProduct(1L, true);
        product.setHasOptions(true);

        Product variation = buildProduct(2L, true);
        variation.setParent(product);
        product.setProducts(List.of(variation));

        ProductOption productOption = new ProductOption();
        productOption.setId(100L);
        productOption.setName("Color");

        ProductOptionCombination combination = ProductOptionCombination.builder()
            .product(variation)
            .productOption(productOption)
            .value("Red")
            .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertEquals(1, result.getVariations().size());
        assertEquals(2L, result.getVariations().getFirst().id());
        assertEquals(Map.of(100L, "Red"), result.getVariations().getFirst().options());
    }

    @Test
    void getProductDetailById_whenVariationNotPublished_shouldExcludeFromVariations() {
        Product product = buildProduct(1L, true);
        product.setHasOptions(true);

        Product unpublishedVariation = buildProduct(2L, false);
        unpublishedVariation.setParent(product);
        product.setProducts(List.of(unpublishedVariation));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getVariations()).isEmpty();
    }

    @Test
    void getProductDetailById_whenProductCategoriesNull_shouldReturnEmptyCategories() {
        Product product = buildProduct(1L, true);
        product.setProductCategories(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getCategories()).isEmpty();
    }

    // --- Helper methods ---

    private Product buildProduct(Long id, boolean published) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product A");
        product.setSku("SKU-" + id);
        product.setSlug("product-a-" + id);
        product.setPublished(published);
        product.setProductCategories(new ArrayList<>());
        product.setAttributeValues(new ArrayList<>());
        product.setProductImages(new ArrayList<>());
        product.setProducts(new ArrayList<>());
        product.setRelatedProducts(new ArrayList<>());
        return product;
    }
}
