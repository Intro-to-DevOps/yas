package com.yas.rating.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.rating.model.Rating;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class RatingVmTest {

    @Test
    void fromModel_whenValidRating_returnRatingVm() {
        ZonedDateTime now = ZonedDateTime.now();
        Rating rating = Rating.builder()
            .id(1L)
            .content("Good")
            .ratingStar(5)
            .productId(10L)
            .productName("Product")
            .firstName("First")
            .lastName("Last")
            .build();
        rating.setCreatedBy("user1");
        rating.setCreatedOn(now);

        RatingVm vm = RatingVm.fromModel(rating);

        assertThat(vm.id()).isEqualTo(1L);
        assertThat(vm.content()).isEqualTo("Good");
        assertThat(vm.star()).isEqualTo(5);
        assertThat(vm.productId()).isEqualTo(10L);
        assertThat(vm.productName()).isEqualTo("Product");
        assertThat(vm.firstName()).isEqualTo("First");
        assertThat(vm.lastName()).isEqualTo("Last");
        assertThat(vm.createdBy()).isEqualTo("user1");
        assertThat(vm.createdOn()).isEqualTo(now);
    }
}
