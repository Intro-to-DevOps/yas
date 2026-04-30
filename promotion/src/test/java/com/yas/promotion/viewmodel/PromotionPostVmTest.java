package com.yas.promotion.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.promotion.model.Promotion;
import com.yas.promotion.model.PromotionApply;
import com.yas.promotion.model.enumeration.ApplyTo;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromotionPostVmTest {

    @Test
    void createPromotionApplies_WhenApplyToProduct_ThenSuccess() {
        Promotion promotion = Promotion.builder().applyTo(ApplyTo.PRODUCT).build();
        PromotionPostVm postVm = PromotionPostVm.builder()
                .productIds(List.of(1L, 2L))
                .build();

        List<PromotionApply> result = PromotionPostVm.createPromotionApplies(postVm, promotion);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getProductId());
    }

    @Test
    void createPromotionApplies_WhenApplyToBrand_ThenSuccess() {
        Promotion promotion = Promotion.builder().applyTo(ApplyTo.BRAND).build();
        PromotionPostVm postVm = PromotionPostVm.builder()
                .brandIds(List.of(10L))
                .build();

        List<PromotionApply> result = PromotionPostVm.createPromotionApplies(postVm, promotion);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getBrandId());
    }

    @Test
    void createPromotionApplies_WhenApplyToCategory_ThenSuccess() {
        Promotion promotion = Promotion.builder().applyTo(ApplyTo.CATEGORY).build();
        PromotionPostVm postVm = PromotionPostVm.builder()
                .categoryIds(List.of(100L))
                .build();

        List<PromotionApply> result = PromotionPostVm.createPromotionApplies(postVm, promotion);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getCategoryId());
    }
}
