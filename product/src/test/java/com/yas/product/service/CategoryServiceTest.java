package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryGetDetailVm;
import com.yas.product.viewmodel.category.CategoryGetVm;
import com.yas.product.viewmodel.category.CategoryListGetVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private CategoryService categoryService;

    private NoFileMediaVm noFileMediaVm;

    @BeforeEach
    void setUp() {
        noFileMediaVm = new NoFileMediaVm(1L, "caption", "fileName", "mediaType", "http://example.com/image.png");
    }

    // --- getPageableCategories ---

    @Test
    void getPageableCategories_shouldReturnPagedResult() {
        Category category = buildCategory(1L, "Cat1", "cat1", null, 1L);
        Page<Category> page = new PageImpl<>(List.of(category), PageRequest.of(0, 10), 1);
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        CategoryListGetVm result = categoryService.getPageableCategories(0, 10);

        assertEquals(1, result.categoryContent().size());
        assertEquals(0, result.pageNo());
        assertEquals(10, result.pageSize());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getPageableCategories_whenEmpty_shouldReturnEmptyList() {
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        CategoryListGetVm result = categoryService.getPageableCategories(0, 10);

        assertThat(result.categoryContent()).isEmpty();
    }

    // --- create ---

    @Test
    void create_whenValidData_shouldSaveAndReturn() {
        CategoryPostVm vm = new CategoryPostVm("NewCat", "new-cat", "desc", null, "kw", "metaDesc", (short) 1, true, null);
        Category savedCategory = buildCategory(10L, "NewCat", "new-cat", null, null);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.create(vm);

        assertEquals("NewCat", result.getName());
    }

    @Test
    void create_whenDuplicateName_shouldThrowDuplicatedException() {
        CategoryPostVm vm = new CategoryPostVm("DupCat", "dup-cat", null, null, null, null, (short) 0, true, null);
        when(categoryRepository.findExistedName("DupCat", null)).thenReturn(new Category());

        assertThrows(DuplicatedException.class, () -> categoryService.create(vm));
    }

    @Test
    void create_whenParentIdProvided_shouldSetParent() {
        Category parent = buildCategory(5L, "Parent", "parent", null, null);
        CategoryPostVm vm = new CategoryPostVm("ChildCat", "child-cat", "desc", 5L, null, null, (short) 0, true, null);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(parent));
        Category savedCategory = buildCategory(11L, "ChildCat", "child-cat", parent, null);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.create(vm);

        assertEquals("Parent", result.getParent().getName());
    }

    @Test
    void create_whenParentNotFound_shouldThrowBadRequestException() {
        CategoryPostVm vm = new CategoryPostVm("ChildCat", "child-cat", "desc", 999L, null, null, (short) 0, true, null);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> categoryService.create(vm));
    }

    // --- update ---

    @Test
    void update_whenValidData_shouldUpdateSuccessfully() {
        Category existing = buildCategory(1L, "OldCat", "old-cat", null, null);
        CategoryPostVm vm = new CategoryPostVm("NewName", "new-name", "desc", null, null, null, (short) 0, true, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        categoryService.update(vm, 1L);

        assertEquals("NewName", existing.getName());
    }

    @Test
    void update_whenCategoryNotFound_shouldThrowNotFoundException() {
        CategoryPostVm vm = new CategoryPostVm("Name", "slug", "desc", null, null, null, (short) 0, true, null);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.update(vm, 99L));
    }

    @Test
    void update_whenDuplicateName_shouldThrowDuplicatedException() {
        CategoryPostVm vm = new CategoryPostVm("DupName", "slug", "desc", null, null, null, (short) 0, true, null);
        when(categoryRepository.findExistedName("DupName", 1L)).thenReturn(new Category());

        assertThrows(DuplicatedException.class, () -> categoryService.update(vm, 1L));
    }

    @Test
    void update_whenParentIdNull_shouldSetParentToNull() {
        Category existing = buildCategory(1L, "Cat", "cat", null, null);
        Category parent = buildCategory(5L, "Parent", "parent", null, null);
        existing.setParent(parent);
        CategoryPostVm vm = new CategoryPostVm("Cat", "cat", "desc", null, null, null, (short) 0, true, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        categoryService.update(vm, 1L);

        // parent should now be null
        assertThat(existing.getParent()).isNull();
    }

    @Test
    void update_whenParentIdIsSelf_shouldThrowBadRequestException() {
        Category existing = buildCategory(1L, "Cat", "cat", null, null);
        // parentId = 1L (same as itself)
        CategoryPostVm vm = new CategoryPostVm("Cat", "cat", "desc", 1L, null, null, (short) 0, true, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        // parent lookup
        Category parentCandidate = buildCategory(1L, "Cat", "cat", null, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Need to set up find to return existing for both id lookups
        // Use a different approach: findById returns existing for both the update and the parent lookup
        assertThrows(BadRequestException.class, () -> categoryService.update(vm, 1L));
    }

    @Test
    void update_whenParentNotFound_shouldThrowBadRequestException() {
        Category existing = buildCategory(1L, "Cat", "cat", null, null);
        CategoryPostVm vm = new CategoryPostVm("Cat", "cat", "desc", 999L, null, null, (short) 0, true, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> categoryService.update(vm, 1L));
    }

    // --- getCategoryById ---

    @Test
    void getCategoryById_whenExists_shouldReturnDetailVm() {
        Category category = buildCategory(1L, "Cat", "cat", null, 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaService.getMedia(1L)).thenReturn(noFileMediaVm);

        CategoryGetDetailVm result = categoryService.getCategoryById(1L);

        assertNotNull(result);
        assertEquals("Cat", result.name());
        assertEquals(0L, result.parentId());
    }

    @Test
    void getCategoryById_whenNotFound_shouldThrowNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getCategoryById(99L));
    }

    @Test
    void getCategoryById_whenHasParent_shouldReturnParentId() {
        Category parent = buildCategory(5L, "Parent", "parent", null, null);
        Category category = buildCategory(1L, "Cat", "cat", parent, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryGetDetailVm result = categoryService.getCategoryById(1L);

        assertEquals(5L, result.parentId());
    }

    @Test
    void getCategoryById_whenNoImage_shouldReturnNullImage() {
        Category category = buildCategory(1L, "Cat", "cat", null, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryGetDetailVm result = categoryService.getCategoryById(1L);

        assertThat(result.categoryImage()).isNull();
    }

    // --- getCategories ---

    @Test
    void getCategories_shouldReturnMatchingCategories() {
        Category category = buildCategory(1L, "Cat1", "cat1", null, 1L);
        when(categoryRepository.findByNameContainingIgnoreCase("Cat")).thenReturn(List.of(category));
        when(mediaService.getMedia(1L)).thenReturn(noFileMediaVm);

        List<CategoryGetVm> result = categoryService.getCategories("Cat");

        assertEquals(1, result.size());
        assertEquals("Cat1", result.get(0).name());
    }

    @Test
    void getCategories_whenCategoryHasParent_shouldReturnParentId() {
        Category parent = buildCategory(5L, "Parent", "parent", null, null);
        Category category = buildCategory(1L, "Cat", "cat", parent, null);
        when(categoryRepository.findByNameContainingIgnoreCase("Cat")).thenReturn(List.of(category));

        List<CategoryGetVm> result = categoryService.getCategories("Cat");

        assertEquals(5L, result.get(0).parentId());
    }

    @Test
    void getCategories_whenCategoryHasNoParent_shouldReturnMinusOne() {
        Category category = buildCategory(1L, "Cat", "cat", null, null);
        when(categoryRepository.findByNameContainingIgnoreCase("Cat")).thenReturn(List.of(category));

        List<CategoryGetVm> result = categoryService.getCategories("Cat");

        assertEquals(-1L, result.get(0).parentId());
    }

    // --- getCategoryByIds ---

    @Test
    void getCategoryByIds_shouldReturnMappedVms() {
        Category category = buildCategory(1L, "Cat1", "cat1", null, null);
        when(categoryRepository.findAllById(List.of(1L))).thenReturn(List.of(category));

        List<CategoryGetVm> result = categoryService.getCategoryByIds(List.of(1L));

        assertEquals(1, result.size());
        assertEquals("Cat1", result.get(0).name());
    }

    // --- getTopNthCategories ---

    @Test
    void getTopNthCategories_shouldReturnLimitedList() {
        when(categoryRepository.findCategoriesOrderedByProductCount(any(Pageable.class)))
            .thenReturn(List.of("CatA", "CatB", "CatC"));

        List<String> result = categoryService.getTopNthCategories(3);

        assertEquals(3, result.size());
        assertEquals("CatA", result.get(0));
    }

    // --- Helper ---

    private Category buildCategory(Long id, String name, String slug, Category parent, Long imageId) {
        Category cat = new Category();
        cat.setId(id);
        cat.setName(name);
        cat.setSlug(slug);
        cat.setParent(parent);
        cat.setImageId(imageId);
        cat.setDisplayOrder((short) 0);
        cat.setIsPublished(true);
        return cat;
    }
}