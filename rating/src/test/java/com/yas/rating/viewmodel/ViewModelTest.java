package com.yas.rating.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.rating.model.Rating;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewModelTest {

    @Test
    void testCustomerVm() {
        CustomerVm vm = new CustomerVm("user", "email@example.com", "First", "Last");
        assertThat(vm.username()).isEqualTo("user");
        assertThat(vm.email()).isEqualTo("email@example.com");
        assertThat(vm.firstName()).isEqualTo("First");
        assertThat(vm.lastName()).isEqualTo("Last");
    }

    @Test
    void testErrorVm() {
        ErrorVm vm = new ErrorVm("400", "Title", "Detail");
        assertThat(vm.statusCode()).isEqualTo("400");
        assertThat(vm.title()).isEqualTo("Title");
        assertThat(vm.detail()).isEqualTo("Detail");
    }

    @Test
    void testOrderExistsByProductAndUserGetVm() {
        OrderExistsByProductAndUserGetVm vm = new OrderExistsByProductAndUserGetVm(true);
        assertThat(vm.isPresent()).isTrue();
    }

    @Test
    void testRatingListVm() {
        RatingVm ratingVm = new RatingVm(1L, "Content", 5, 1L, "Product", "User", "Last", "First", ZonedDateTime.now());
        RatingListVm vm = new RatingListVm(List.of(ratingVm), 1L, 1);
        assertThat(vm.ratingList()).hasSize(1);
        assertThat(vm.totalElements()).isEqualTo(1L);
        assertThat(vm.totalPages()).isEqualTo(1);
    }

    @Test
    void testRatingPostVm() {
        RatingPostVm vm = new RatingPostVm("Content", 5, 1L, "Product");
        assertThat(vm.content()).isEqualTo("Content");
        assertThat(vm.star()).isEqualTo(5);
        assertThat(vm.productId()).isEqualTo(1L);
        assertThat(vm.productName()).isEqualTo("Product");

        RatingPostVm vmBuilder = RatingPostVm.builder()
                .content("Content")
                .star(5)
                .productId(1L)
                .productName("Product")
                .build();
        assertThat(vmBuilder.content()).isEqualTo("Content");
    }

    @Test
    void testResponeStatusVm() {
        ResponeStatusVm vm = new ResponeStatusVm("Title", "Message", "200");
        assertThat(vm.title()).isEqualTo("Title");
        assertThat(vm.message()).isEqualTo("Message");
        assertThat(vm.statusCode()).isEqualTo("200");
    }

    @Test
    void testRatingVm() {
        ZonedDateTime now = ZonedDateTime.now();
        RatingVm vm = new RatingVm(1L, "Content", 5, 1L, "Product", "User", "Last", "First", now);
        assertThat(vm.id()).isEqualTo(1L);
        assertThat(vm.content()).isEqualTo("Content");
        assertThat(vm.star()).isEqualTo(5);
        assertThat(vm.productId()).isEqualTo(1L);
        assertThat(vm.productName()).isEqualTo("Product");
        assertThat(vm.createdBy()).isEqualTo("User");
        assertThat(vm.lastName()).isEqualTo("Last");
        assertThat(vm.firstName()).isEqualTo("First");
        assertThat(vm.createdOn()).isEqualTo(now);

        Rating rating = Rating.builder()
                .id(1L)
                .content("Content")
                .ratingStar(5)
                .productId(1L)
                .productName("Product")
                .lastName("Last")
                .firstName("First")
                .build();
        rating.setCreatedBy("User");
        rating.setCreatedOn(now);

        RatingVm fromModel = RatingVm.fromModel(rating);
        assertThat(fromModel.id()).isEqualTo(1L);
        assertThat(fromModel.content()).isEqualTo("Content");
        assertThat(fromModel.star()).isEqualTo(5);
    }
}
