package com.yas.promotion.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.promotion.model.Promotion;
import com.yas.promotion.model.enumeration.ApplyTo;
import com.yas.promotion.model.enumeration.DiscountType;
import com.yas.promotion.model.enumeration.UsageType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PromotionVmTest {

    @Test
    void testPromotionVmBuilderAndMapping() {
        Instant now = Instant.now();
        PromotionVm vm = PromotionVm.builder()
                .id(1L)
                .name("Test Promo")
                .slug("test-promo")
                .couponCode("TEST")
                .isActive(true)
                .startDate(now)
                .endDate(now.plusSeconds(3600))
                .build();

        assertEquals(1L, vm.id());
        assertEquals("Test Promo", vm.name());
        assertEquals("test-promo", vm.slug());
        assertEquals("TEST", vm.couponCode());
        assertEquals(true, vm.isActive());
        assertEquals(now, vm.startDate());
    }

    @Test
    void testFromModel() {
        Instant now = Instant.now();
        Promotion promotion = Promotion.builder()
                .id(1L)
                .name("Test Promo")
                .slug("test-promo")
                .couponCode("TEST")
                .usageType(UsageType.LIMITED)
                .usageLimit(100)
                .usageCount(10)
                .isActive(true)
                .startDate(now)
                .endDate(now.plusSeconds(3600))
                .build();

        PromotionVm vm = PromotionVm.fromModel(promotion);

        assertEquals(1L, vm.id());
        assertEquals("Test Promo", vm.name());
        assertEquals("test-promo", vm.slug());
        assertEquals(true, vm.isActive());
    }
}
