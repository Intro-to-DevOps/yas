package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductRelated;
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
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductQuantityPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductsGetVm;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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

    // =================== getProductCheckoutList ===================

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

    // =================== getProductById ===================

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
    void getProductById_whenNoImages_shouldReturnEmptyImageList() {
        Product product = createProduct();
        product.setProductImages(null);
        product.setThumbnailMediaId(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertThat(result.productImageMedias()).isEmpty();
        assertNull(result.thumbnailMedia());
    }

    @Test
    void getProductById_whenNoBrand_shouldReturnNullBrandId() {
        Product product = createProduct();
        product.setBrand(null);
        product.setProductImages(null);
        product.setThumbnailMediaId(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertNull(result.brandId());
    }

    // =================== getLatestProducts ===================

    @Test
    void getLatestProducts_whenCountLessOrEqualZero_shouldReturnEmpty() {
        assertThat(productService.getLatestProducts(0)).isEmpty();
        assertThat(productService.getLatestProducts(-1)).isEmpty();
    }

    @Test
    void getLatestProducts_whenRepositoryReturnsData_shouldMapProducts() {
        Product product = createProduct();
        when(productRepository.getLatestProducts(PageRequest.of(0, 1))).thenReturn(List.of(product));

        assertThat(productService.getLatestProducts(1)).hasSize(1);
        assertEquals("Product A", productService.getLatestProducts(1).getFirst().name());
    }

    @Test
    void getLatestProducts_whenRepositoryReturnsEmpty_shouldReturnEmpty() {
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of());

        assertThat(productService.getLatestProducts(5)).isEmpty();
    }

    // =================== getProductsByBrand ===================

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

    // =================== deleteProduct ===================

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
    void deleteProduct_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.deleteProduct(999L));
    }

    @Test
    void deleteProduct_whenNoParent_shouldSoftDeleteWithoutRemovingCombinations() {
        Product product = new Product();
        product.setId(1L);
        product.setPublished(true);
        product.setParent(null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        productService.deleteProduct(1L);

        assertThat(product.isPublished()).isFalse();
        verify(productRepository).save(product);
    }

    // =================== getProductSlug ===================

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

    @Test
    void getProductSlug_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductSlug(999L));
    }

    // =================== getProductsWithFilter ===================

    @Test
    void getProductsWithFilter_shouldReturnPagedResult() {
        Product product = createProduct();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.getProductsWithFilter(any(), any(), any(Pageable.class))).thenReturn(page);

        ProductListGetVm result = productService.getProductsWithFilter(0, 10, "Product", "Brand");

        assertEquals(1, result.productContent().size());
        assertEquals(0, result.pageNo());
    }

    // =================== getProductsByMultiQuery ===================

    @Test
    void getProductsByMultiQuery_shouldReturnPagedThumbnails() {
        Product product = createProduct();
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(page);
        when(mediaService.getMedia(product.getThumbnailMediaId()))
            .thenReturn(mediaVm(10L, "http://cdn.example/img.jpg"));

        ProductsGetVm result = productService.getProductsByMultiQuery(0, 10, "prod", "cat", 0.0, 1000.0);

        assertEquals(1, result.productContent().size());
    }

    // =================== getProductEsDetailById ===================

    @Test
    void getProductEsDetailById_whenExists_shouldReturnEsVm() {
        Product product = createDetailProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(1L);

        assertEquals(1L, result.id());
        assertEquals("Product A", result.name());
    }

    @Test
    void getProductEsDetailById_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductEsDetailById(999L));
    }

    @Test
    void getProductEsDetailById_whenNoBrand_shouldReturnNullBrandName() {
        Product product = createProduct();
        product.setBrand(null);
        product.setProductImages(null);
        product.setProductCategories(new ArrayList<>());
        product.setAttributeValues(new ArrayList<>());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(1L);

        assertNull(result.brand());
    }

    // =================== getRelatedProductsBackoffice ===================

    @Test
    void getRelatedProductsBackoffice_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getRelatedProductsBackoffice(99L));
    }

    @Test
    void getRelatedProductsBackoffice_whenExists_shouldReturnRelatedProductList() {
        Product relatedProduct = createProduct();
        relatedProduct.setId(2L);
        Product product = createProduct();
        ProductRelated related = ProductRelated.builder()
            .product(product)
            .relatedProduct(relatedProduct)
            .build();
        product.setRelatedProducts(List.of(related));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        List<ProductListVm> result = productService.getRelatedProductsBackoffice(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
    }

    // =================== getRelatedProductsStorefront ===================

    @Test
    void getRelatedProductsStorefront_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getRelatedProductsStorefront(99L, 0, 10));
    }

    @Test
    void getRelatedProductsStorefront_shouldReturnOnlyPublishedProducts() {
        Product relatedProduct = createProduct();
        relatedProduct.setPublished(true);
        Product product = createProduct();
        ProductRelated related = ProductRelated.builder()
            .product(product)
            .relatedProduct(relatedProduct)
            .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRelatedRepository.findAllByProduct(product, PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(related)));
        when(mediaService.getMedia(relatedProduct.getThumbnailMediaId()))
            .thenReturn(mediaVm(10L, "https://cdn.example/thumb.jpg"));

        ProductsGetVm result = productService.getRelatedProductsStorefront(1L, 0, 10);

        assertEquals(1, result.productContent().size());
    }

    // =================== updateProductQuantity ===================

    @Test
    void updateProductQuantity_shouldSetStockQuantity() {
        Product product = createProduct();
        product.setId(1L);
        ProductQuantityPostVm vm = new ProductQuantityPostVm(1L, 50L);

        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenReturn(List.of(product));

        productService.updateProductQuantity(List.of(vm));

        assertEquals(50L, product.getStockQuantity());
    }

    // =================== subtractStockQuantity ===================

    @Test
    void subtractStockQuantity_shouldDecreaseStock() {
        Product product = createProduct();
        product.setId(1L);
        product.setStockQuantity(100L);
        product.setStockTrackingEnabled(true);

        ProductQuantityPutVm vm = new ProductQuantityPutVm(1L, 30L);

        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenReturn(List.of(product));

        productService.subtractStockQuantity(List.of(vm));

        assertEquals(70L, product.getStockQuantity());
    }

    @Test
    void subtractStockQuantity_whenResultNegative_shouldSetToZero() {
        Product product = createProduct();
        product.setId(1L);
        product.setStockQuantity(10L);
        product.setStockTrackingEnabled(true);

        ProductQuantityPutVm vm = new ProductQuantityPutVm(1L, 100L);

        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenReturn(List.of(product));

        productService.subtractStockQuantity(List.of(vm));

        assertEquals(0L, product.getStockQuantity());
    }

    // =================== restoreStockQuantity ===================

    @Test
    void restoreStockQuantity_shouldIncreaseStock() {
        Product product = createProduct();
        product.setId(1L);
        product.setStockQuantity(50L);
        product.setStockTrackingEnabled(true);

        ProductQuantityPutVm vm = new ProductQuantityPutVm(1L, 20L);

        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenReturn(List.of(product));

        productService.restoreStockQuantity(List.of(vm));

        assertEquals(70L, product.getStockQuantity());
    }

    // =================== getProductByIds ===================

    @Test
    void getProductByIds_shouldReturnListOfVms() {
        Product product = createProduct();
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getProductByIds(List.of(1L));

        assertEquals(1, result.size());
        assertEquals("Product A", result.get(0).name());
    }

    // =================== getProductByCategoryIds ===================

    @Test
    void getProductByCategoryIds_shouldReturnMappedVms() {
        Product product = createProduct();
        when(productRepository.findByCategoryIdsIn(List.of(5L))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getProductByCategoryIds(List.of(5L));

        assertEquals(1, result.size());
    }

    // =================== getProductByBrandIds ===================

    @Test
    void getProductByBrandIds_shouldReturnMappedVms() {
        Product product = createProduct();
        when(productRepository.findByBrandIdsIn(List.of(2L))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getProductByBrandIds(List.of(2L));

        assertEquals(1, result.size());
    }

    // =================== getFeaturedProductsById ===================

    @Test
    void getFeaturedProductsById_whenThumbnailUrlNotEmpty_shouldReturnWithThumbnailUrl() {
        Product product = createProduct();
        product.setParent(null);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        when(mediaService.getMedia(10L)).thenReturn(mediaVm(10L, "https://cdn.example/thumb.jpg"));

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(1L));

        assertEquals(1, result.size());
        assertEquals("https://cdn.example/thumb.jpg", result.get(0).thumbnailUrl());
    }

    @Test
    void getFeaturedProductsById_whenThumbnailUrlEmpty_andHasParent_shouldUseParentThumbnail() {
        Product parent = createProduct();
        parent.setId(99L);
        parent.setThumbnailMediaId(99L);

        Product product = createProduct();
        product.setParent(parent);
        product.setThumbnailMediaId(10L);

        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        // Empty thumbnail for child
        when(mediaService.getMedia(10L)).thenReturn(mediaVm(10L, ""));
        // But parent has thumbnail
        when(productRepository.findById(99L)).thenReturn(Optional.of(parent));
        when(mediaService.getMedia(99L)).thenReturn(mediaVm(99L, "https://cdn.example/parent-thumb.jpg"));

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(1L));

        assertEquals(1, result.size());
        assertEquals("https://cdn.example/parent-thumb.jpg", result.get(0).thumbnailUrl());
    }

    // =================== setProductImages ===================

    @Test
    void setProductImages_whenImageIdsEmpty_shouldDeleteExistingAndReturnEmpty() {
        Product product = createProduct();

        List<ProductImage> result = productService.setProductImages(List.of(), product);

        assertThat(result).isEmpty();
        verify(productImageRepository).deleteByProductId(product.getId());
    }

    @Test
    void setProductImages_whenProductHasNoExistingImages_shouldCreateNew() {
        Product product = createProduct();
        product.setProductImages(null);

        List<ProductImage> result = productService.setProductImages(List.of(100L, 200L), product);

        assertEquals(2, result.size());
    }

    // =================== Helper methods ===================

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
        product.setProductCategories(new ArrayList<>());
        product.setAttributeValues(new ArrayList<>());
        product.setProductImages(new ArrayList<>());
        product.setRelatedProducts(new ArrayList<>());
        product.setProducts(new ArrayList<>());
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

    // =================== getProductsFromCategory ===================

    @Test
    void getProductsFromCategory_whenCategoryExists_shouldReturnProducts() {
        Category category = new Category();
        category.setId(1L);
        category.setSlug("electronics");

        Product product = createProduct();
        ProductCategory productCategory = ProductCategory.builder()
            .category(category)
            .product(product)
            .build();

        when(categoryRepository.findBySlug("electronics")).thenReturn(java.util.Optional.of(category));
        when(productCategoryRepository.findAllByCategory(any(org.springframework.data.domain.Pageable.class), any(Category.class)))
            .thenReturn(new PageImpl<>(List.of(productCategory)));
        when(mediaService.getMedia(10L)).thenReturn(mediaVm(10L, "http://cdn.example/thumb.jpg"));

        var result = productService.getProductsFromCategory(0, 10, "electronics");

        assertEquals(1, result.productContent().size());
    }

    @Test
    void getProductsFromCategory_whenCategoryNotFound_shouldThrowNotFoundException() {
        when(categoryRepository.findBySlug("nonexistent")).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsFromCategory(0, 10, "nonexistent"));
    }

    // =================== getListFeaturedProducts ===================

    @Test
    void getListFeaturedProducts_shouldReturnFeaturedProducts() {
        Product product = createProduct();
        when(productRepository.getFeaturedProduct(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product)));
        when(mediaService.getMedia(10L)).thenReturn(mediaVm(10L, "http://cdn.example/thumb.jpg"));

        var result = productService.getListFeaturedProducts(0, 10);

        assertEquals(1, result.productList().size());
        assertEquals(1, result.totalPage());
    }

    // =================== exportProducts ===================

    @Test
    void exportProducts_shouldReturnExportDetails() {
        Brand brand = new Brand();
        brand.setId(2L);
        brand.setName("Brand A");

        Product product = createProduct();
        product.setBrand(brand);

        when(productRepository.getExportingProducts(any(), any())).thenReturn(List.of(product));

        var result = productService.exportProducts("Product", "Brand");

        assertEquals(1, result.size());
        assertEquals("Product A", result.getFirst().name());
    }

    // =================== getProductVariationsByParentId ===================

    @Test
    void getProductVariationsByParentId_whenParentNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductVariationsByParentId(99L));
    }

    @Test
    void getProductVariationsByParentId_whenParentHasNoOptions_shouldReturnEmpty() {
        Product parent = createProduct();
        parent.setHasOptions(false);
        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(parent));

        List<com.yas.product.viewmodel.product.ProductVariationGetVm> result =
            productService.getProductVariationsByParentId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductVariationsByParentId_whenParentHasOptions_shouldReturnVariations() {
        Product parent = createProduct();
        parent.setHasOptions(true);

        Product variation = createProduct();
        variation.setId(2L);
        variation.setPublished(true);
        variation.setThumbnailMediaId(null);
        variation.setProductImages(new ArrayList<>());

        com.yas.product.model.ProductOption productOption = new com.yas.product.model.ProductOption();
        productOption.setId(100L);
        productOption.setName("Color");

        ProductOptionCombination combination = ProductOptionCombination.builder()
            .product(variation)
            .productOption(productOption)
            .value("Blue")
            .build();

        parent.setProducts(List.of(variation));

        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(parent));
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));

        List<com.yas.product.viewmodel.product.ProductVariationGetVm> result =
            productService.getProductVariationsByParentId(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().id());
    }

    // =================== getProductsForWarehouse ===================

    @Test
    void getProductsForWarehouse_shouldReturnFilteredProducts() {
        Product product = createProduct();
        when(productRepository.findProductForWarehouse(any(), any(), anyList(), any()))
            .thenReturn(List.of(product));

        var result = productService.getProductsForWarehouse(
            "Product", "SKU-1", List.of(1L),
            com.yas.product.model.enumeration.FilterExistInWhSelection.ALL);

        assertEquals(1, result.size());
    }

    // =================== createProduct ===================

    @Test
    void createProduct_whenNoVariations_shouldSaveAndReturnVm() {
        com.yas.product.viewmodel.product.ProductPostVm vm = new com.yas.product.viewmodel.product.ProductPostVm(
            "Product A", "product-a", 2L, List.of(), "short desc", "desc", "spec",
            "SKU-A", null, 1.0, null, 2.0, 1.0, 1.0, 99.99, true, true, false, true,
            true, "title", "keyword", "meta-desc", 10L, List.of(), List.of(), List.of(),
            List.of(), null, null
        );

        Brand brand = new Brand();
        brand.setId(2L);

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Product A");
        savedProduct.setSlug("product-a");

        when(brandRepository.findById(2L)).thenReturn(java.util.Optional.of(brand));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productImageRepository.saveAll(anyList())).thenReturn(List.of());
        when(productCategoryRepository.saveAll(anyList())).thenReturn(List.of());
        when(productRepository.findAllById(anyList())).thenReturn(List.of());

        var result = productService.createProduct(vm);

        assertEquals(1L, result.id());
        assertEquals("Product A", result.name());
    }

    @Test
    void createProduct_whenBrandNotFound_shouldThrowNotFoundException() {
        com.yas.product.viewmodel.product.ProductPostVm vm = new com.yas.product.viewmodel.product.ProductPostVm(
            "Product A", "product-a", 999L, List.of(), "short desc", "desc", "spec",
            "SKU-A", null, 1.0, null, 2.0, 1.0, 1.0, 99.99, true, true, false, true,
            true, "title", "keyword", "meta-desc", null, List.of(), List.of(), List.of(),
            List.of(), null, null
        );

        when(brandRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.createProduct(vm));
    }

    @Test
    void createProduct_whenDuplicateSlug_shouldThrowDuplicatedException() {
        com.yas.product.viewmodel.product.ProductPostVm vm = new com.yas.product.viewmodel.product.ProductPostVm(
            "Product A", "existing-slug", null, List.of(), "short desc", "desc", "spec",
            "SKU-NEW", null, 1.0, null, 2.0, 1.0, 1.0, 99.99, true, true, false, true,
            true, "title", "keyword", "meta-desc", null, List.of(), List.of(), List.of(),
            List.of(), null, null
        );

        Product existing = new Product();
        existing.setId(99L);
        when(productRepository.findBySlugAndIsPublishedTrue("existing-slug")).thenReturn(java.util.Optional.of(existing));
        when(productRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(DuplicatedException.class, () -> productService.createProduct(vm));
    }

    @Test
    void createProduct_whenLengthLessThanWidth_shouldThrowBadRequestException() {
        com.yas.product.viewmodel.product.ProductPostVm vm = new com.yas.product.viewmodel.product.ProductPostVm(
            "Product A", "product-a", null, List.of(), "short desc", "desc", "spec",
            "SKU-A", null, 1.0, null, 1.0, 2.0, 1.0, 99.99, true, true, false, true,
            true, "title", "keyword", "meta-desc", null, List.of(), List.of(), List.of(),
            List.of(), null, null
        );

        assertThrows(BadRequestException.class, () -> productService.createProduct(vm));
    }

    // =================== updateProduct ===================

    @Test
    void updateProduct_whenProductNotFound_shouldThrowNotFoundException() {
        com.yas.product.viewmodel.product.ProductPutVm vm = new com.yas.product.viewmodel.product.ProductPutVm(
            "Product A", "product-a", 99.99, true, true, false, true, true,
            null, List.of(), "short desc", "desc", "spec", "SKU-A", null,
            1.0, null, 2.0, 1.0, 1.0, "title", "keyword", "meta-desc", null,
            List.of(), List.of(), List.of(), List.of(), null
        );

        when(productRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.updateProduct(999L, vm));
    }

    // =================== getProductDetail ===================

    @Test
    void getProductDetail_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findBySlugAndIsPublishedTrue("nonexistent-slug"))
            .thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductDetail("nonexistent-slug"));
    }

    @Test
    void getProductDetail_whenProductHasNoImages_shouldReturnEmptyImageList() {
        Product product = createProduct();
        product.setSlug("product-a");
        product.setBrand(null);
        product.setProductImages(List.of());
        product.setAttributeValues(new ArrayList<>());
        product.setProductCategories(new ArrayList<>());

        when(productRepository.findBySlugAndIsPublishedTrue("product-a"))
            .thenReturn(java.util.Optional.of(product));
        when(mediaService.getMedia(product.getThumbnailMediaId()))
            .thenReturn(mediaVm(10L, "http://cdn.example/thumbnail.jpg"));

        var result = productService.getProductDetail("product-a");

        assertEquals("Product A", result.name());
        assertNull(result.brandName());
        assertThat(result.productImageMediaUrls()).isEmpty();
        assertThat(result.productAttributeGroups()).isEmpty();
    }
}
