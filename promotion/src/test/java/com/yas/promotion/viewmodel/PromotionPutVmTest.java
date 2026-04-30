package com.yas.promotion.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.promotion.model.Promotion;
import com.yas.promotion.model.PromotionApply;
import com.yas.promotion.model.enumeration.ApplyTo;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromotionPutVmTest {

    @Test
    void createPromotionApplies_WhenApplyToProduct_ThenSuccess() {
        Promotion promotion = Promotion.builder().applyTo(ApplyTo.PRODUCT).build();
        PromotionPutVm putVm = PromotionPutVm.builder()
                .productIds(List.of(1L, 2L))
                .build();

        List<PromotionApply> result = PromotionPutVm.createPromotionApplies(putVm, promotion);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getProductId());
        assertEquals(2L, result.get(1).getProductId());
    }

    @Test
    void createPromotionApplies_WhenApplyToBrand_ThenSuccess() {
        Promotion promotion = Promotion.builder().applyTo(ApplyTo.BRAND).build();
        PromotionPutVm putVm = PromotionPutVm.builder()
                .brandIds(List.of(10L))
                .build();

        List<PromotionApply> result = PromotionPutVm.createPromotionApplies(putVm, promotion);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getBrandId());
    }

    @Test
    void createPromotionApplies_WhenApplyToCategory_ThenSuccess() {
        Promotion promotion = Promotion.builder().applyTo(ApplyTo.CATEGORY).build();
        PromotionPutVm putVm = PromotionPutVm.builder()
                .categoryIds(List.of(100L, 200L))
                .build();

        List<PromotionApply> result = PromotionPutVm.createPromotionApplies(putVm, promotion);

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getCategoryId());
        assertEquals(200L, result.get(1).getCategoryId());
    }
}
