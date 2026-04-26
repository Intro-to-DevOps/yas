package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.ImageVm;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private ProductOptionValueRepository productOptionValueRepository;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @Mock
    private ProductRelatedRepository productRelatedRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
            productRepository,
            mediaService,
            brandRepository,
            productCategoryRepository,
            categoryRepository,
            productImageRepository,
            productOptionRepository,
            productOptionValueRepository,
            productOptionCombinationRepository,
            productRelatedRepository
        );
    }

    @Test
    void getProductCheckoutList_whenThumbnailUrlExists_shouldPopulateThumbnailUrl() {
        Product product = createProduct();
        when(productRepository.findAllPublishedProductsByIds(anyList(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));
        when(mediaService.getMedia(product.getThumbnailMediaId()))
            .thenReturn(new NoFileMediaVm(1L, "caption", "thumbnail.jpg", "image/jpeg", "https://cdn.example/thumbnail.jpg"));

        ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 10, List.of(product.getId()));

        assertThat(result.productCheckoutListVms()).hasSize(1);
        assertThat(result.productCheckoutListVms().getFirst().thumbnailUrl()).isEqualTo("https://cdn.example/thumbnail.jpg");
        assertThat(result.pageNo()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getProductCheckoutList_whenThumbnailUrlMissing_shouldKeepDefaultThumbnailUrl() {
        Product product = createProduct();
        when(productRepository.findAllPublishedProductsByIds(anyList(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));
        when(mediaService.getMedia(product.getThumbnailMediaId()))
            .thenReturn(new NoFileMediaVm(1L, "caption", "thumbnail.jpg", "image/jpeg", ""));

        ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 10, List.of(product.getId()));

        assertThat(result.productCheckoutListVms()).hasSize(1);
        assertThat(result.productCheckoutListVms().getFirst().thumbnailUrl()).isEmpty();
    }

    @Test
    void getProductById_whenProductExists_shouldReturnDetailVm() {
        Product product = createDetailProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(10L)).thenReturn(mediaVm(10L, "https://cdn.example/thumb.jpg"));
        when(mediaService.getMedia(20L)).thenReturn(mediaVm(20L, "https://cdn.example/image-20.jpg"));
        when(mediaService.getMedia(21L)).thenReturn(mediaVm(21L, "https://cdn.example/image-21.jpg"));

        ProductDetailVm result = productService.getProductById(1L);

        assertEquals(1L, result.id());
        assertEquals("Product A", result.name());
        assertEquals(2L, result.brandId());
        assertEquals(1, result.categories().size());
        assertEquals("Category A", result.categories().getFirst().getName());
        assertEquals(2, result.productImageMedias().size());
        assertEquals(99L, result.parentId());
        assertEquals("https://cdn.example/thumb.jpg", result.thumbnailMedia().url());
        assertThat(result.productImageMedias())
            .extracting(ImageVm::url)
            .containsExactly("https://cdn.example/image-20.jpg", "https://cdn.example/image-21.jpg");
    }

    @Test
    void getProductById_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    void getLatestProducts_whenCountLessOrEqualZero_shouldReturnEmpty() {
        assertThat(productService.getLatestProducts(0)).isEmpty();
    }

    @Test
    void getLatestProducts_whenRepositoryReturnsData_shouldMapProducts() {
        Product product = createProduct();
        when(productRepository.getLatestProducts(PageRequest.of(0, 1))).thenReturn(List.of(product));

        assertThat(productService.getLatestProducts(1)).hasSize(1);
        assertEquals("Product A", productService.getLatestProducts(1).getFirst().name());
    }

    @Test
    void getProductsByBrand_whenBrandMissing_shouldThrowNotFoundException() {
        when(brandRepository.findBySlug("brand-a")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsByBrand("brand-a"));
    }

    @Test
    void getProductsByBrand_whenBrandExists_shouldReturnThumbnails() {
        Brand brand = new Brand();
        brand.setId(2L);
        brand.setSlug("brand-a");
        brand.setName("Brand A");

        Product product = createProduct();
        when(brandRepository.findBySlug("brand-a")).thenReturn(Optional.of(brand));
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(product));
        when(mediaService.getMedia(product.getThumbnailMediaId())).thenReturn(mediaVm(10L, "https://cdn.example/thumbnail.jpg"));

        List<ProductThumbnailVm> result = productService.getProductsByBrand("brand-a");

        assertThat(result).hasSize(1);
        assertEquals(1L, result.getFirst().id());
        assertEquals("https://cdn.example/thumbnail.jpg", result.getFirst().thumbnailUrl());
    }

    @Test
    void deleteProduct_whenVariationProductExists_shouldDeleteOptionCombinationsAndSoftDelete() {
        Product parent = new Product();
        parent.setId(1L);
        Product variation = new Product();
        variation.setId(2L);
        variation.setPublished(true);
        variation.setParent(parent);
        ProductOptionCombination combination = ProductOptionCombination.builder().product(variation).build();

        when(productRepository.findById(2L)).thenReturn(Optional.of(variation));
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));
        when(productRepository.save(variation)).thenAnswer(invocation -> invocation.getArgument(0));

        productService.deleteProduct(2L);

        assertThat(variation.isPublished()).isFalse();
        verify(productOptionCombinationRepository).deleteAll(List.of(combination));
        verify(productRepository).save(variation);
    }

    @Test
    void getProductSlug_whenProductHasParent_shouldReturnParentSlugAndVariantId() {
        Product parent = new Product();
        parent.setId(100L);
        parent.setSlug("parent-slug");
        Product product = new Product();
        product.setId(1L);
        product.setSlug("child-slug");
        product.setParent(parent);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductSlugGetVm result = productService.getProductSlug(1L);

        assertEquals("parent-slug", result.slug());
        assertEquals(1L, result.productVariantId());
    }

    @Test
    void getProductSlug_whenProductHasNoParent_shouldReturnOwnSlug() {
        Product product = new Product();
        product.setId(1L);
        product.setSlug("parent-slug");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductSlugGetVm result = productService.getProductSlug(1L);

        assertEquals("parent-slug", result.slug());
        assertNull(result.productVariantId());
    }

    private Product createProduct() {
        Brand brand = new Brand();
        brand.setId(2L);
        brand.setName("Brand A");

        Product product = new Product();
        product.setId(1L);
        product.setName("Product A");
        product.setDescription("Description");
        product.setShortDescription("Short description");
        product.setSku("SKU-1");
        product.setBrand(brand);
        product.setPrice(99.99);
        product.setTaxClassId(1L);
        product.setThumbnailMediaId(10L);
        product.setCreatedOn(ZonedDateTime.now());
        product.setCreatedBy("tester");
        return product;
    }

    private Product createDetailProduct() {
        Product product = createProduct();
        product.setSlug("product-a");
        product.setProductImages(List.of(
            ProductImage.builder().imageId(20L).build(),
            ProductImage.builder().imageId(21L).build()
        ));
        Category category = new Category();
        category.setId(5L);
        category.setName("Category A");
        product.setProductCategories(List.of(ProductCategory.builder().category(category).build()));
        Product parent = new Product();
        parent.setId(99L);
        product.setParent(parent);
        return product;
    }

    private NoFileMediaVm mediaVm(Long id, String url) {
        return new NoFileMediaVm(id, "caption", "file", "image/jpeg", url);
    }
}



