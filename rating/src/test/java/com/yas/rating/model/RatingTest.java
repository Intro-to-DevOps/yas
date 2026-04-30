package com.yas.rating.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void testEquals_whenSameId_returnTrue() {
        Rating r1 = new Rating();
        r1.setId(1L);
        Rating r2 = new Rating();
        r2.setId(1L);

        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void testEquals_whenDifferentId_returnFalse() {
        Rating r1 = new Rating();
        r1.setId(1L);
        Rating r2 = new Rating();
        r2.setId(2L);

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void testEquals_whenSameInstance_returnTrue() {
        Rating r1 = new Rating();
        assertThat(r1).isEqualTo(r1);
    }

    @Test
    void testEquals_whenNullOrDifferentClass_returnFalse() {
        Rating r1 = new Rating();
        assertThat(r1).isNotEqualTo(null);
        assertThat(r1).isNotEqualTo("string");
    }

    @Test
    void testHashCode_returnConstant() {
        Rating r1 = new Rating();
        Rating r2 = new Rating();
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testGettersAndSetters() {
        Rating rating = new Rating();
        rating.setId(1L);
        rating.setContent("content");
        rating.setRatingStar(5);
        rating.setProductId(1L);
        rating.setProductName("product");
        rating.setFirstName("first");
        rating.setLastName("last");

        assertThat(rating.getId()).isEqualTo(1L);
        assertThat(rating.getContent()).isEqualTo("content");
        assertThat(rating.getRatingStar()).isEqualTo(5);
        assertThat(rating.getProductId()).isEqualTo(1L);
        assertThat(rating.getProductName()).isEqualTo("product");
        assertThat(rating.getFirstName()).isEqualTo("first");
        assertThat(rating.getLastName()).isEqualTo("last");
    }
}
