package com.yas.payment.mapper;

import com.yas.payment.model.PaymentProvider;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class MapperTest {

    private final CreatePaymentProviderMapper createMapper = Mappers.getMapper(CreatePaymentProviderMapper.class);
    private final PaymentProviderMapper paymentProviderMapper = Mappers.getMapper(PaymentProviderMapper.class);
    private final UpdatePaymentProviderMapper updateMapper = Mappers.getMapper(UpdatePaymentProviderMapper.class);

    @Test
    void createMapper_toModel_withNull_shouldReturnNull() {
        assertNull(createMapper.toModel(null));
    }

    @Test
    void createMapper_toModel_withValidVm_shouldMapAllFields() {
        CreatePaymentVm vm = new CreatePaymentVm();
        vm.setId("paypal");
        vm.setName("PayPal");
        vm.setConfigureUrl("http://config");
        vm.setEnabled(true);
        vm.setMediaId(100L);
        vm.setLandingViewComponentName("PaypalView");
        vm.setAdditionalSettings("settings");

        PaymentProvider model = createMapper.toModel(vm);

        assertNotNull(model);
        assertEquals("paypal", model.getId());
        assertEquals("PayPal", model.getName());
        assertEquals("http://config", model.getConfigureUrl());
        assertTrue(model.isEnabled());
        assertEquals(100L, model.getMediaId());
        assertEquals("PaypalView", model.getLandingViewComponentName());
        assertEquals("settings", model.getAdditionalSettings());
        assertTrue(model.isNew());
    }

    @Test
    void createMapper_toVm_withNull_shouldReturnNull() {
        assertNull(createMapper.toVm(null));
    }

    @Test
    void createMapper_toVm_shouldMapAllFields() {
        PaymentProvider model = PaymentProvider.builder()
                .id("paypal")
                .name("PayPal")
                .enabled(true)
                .configureUrl("http://config")
                .mediaId(100L)
                .build();
        
        CreatePaymentVm vm = createMapper.toVm(model);
        assertNotNull(vm);
        assertEquals("paypal", vm.getId());
        assertTrue(vm.isEnabled());
    }

    @Test
    void createMapper_partialUpdate_shouldMapOnlyNonNullFields() {
        PaymentProvider model = new PaymentProvider();
        CreatePaymentVm vm = new CreatePaymentVm();
        
        // Test with null vm
        createMapper.partialUpdate(model, null);
        
        // Test with all null fields in vm
        createMapper.partialUpdate(model, vm);
        assertNull(model.getId());
        
        // Test with mixed fields
        vm.setId("paypal");
        vm.setName(null); // Should not update
        vm.setConfigureUrl("http://config");
        vm.setMediaId(200L);
        vm.setLandingViewComponentName("Comp");
        vm.setAdditionalSettings("Settings");
        
        model.setName("Old Name");
        createMapper.partialUpdate(model, vm);
        
        assertEquals("paypal", model.getId());
        assertEquals("Old Name", model.getName());
        assertEquals("http://config", model.getConfigureUrl());
        assertEquals(200L, model.getMediaId());
        assertEquals("Comp", model.getLandingViewComponentName());
        assertEquals("Settings", model.getAdditionalSettings());
    }

    @Test
    void createMapper_toVmResponse_withNull_shouldReturnNull() {
        assertNull(createMapper.toVmResponse(null));
    }

    @Test
    void createMapper_toVmResponse_shouldMapAllFields() {
        PaymentProvider model = PaymentProvider.builder()
                .id("paypal")
                .name("PayPal")
                .version(1)
                .build();
        PaymentProviderVm vm = createMapper.toVmResponse(model);
        assertNotNull(vm);
        assertEquals("paypal", vm.getId());
        assertEquals(1, vm.getVersion());
    }

    @Test
    void paymentProviderMapper_toVm_withNull_shouldReturnNull() {
        assertNull(paymentProviderMapper.toVm(null));
    }

    @Test
    void paymentProviderMapper_toModel_withNull_shouldReturnNull() {
        assertNull(paymentProviderMapper.toModel(null));
    }

    @Test
    void paymentProviderMapper_toVm_shouldMapFields() {
        PaymentProvider model = PaymentProvider.builder().id("p1").build();
        PaymentProviderVm vm = paymentProviderMapper.toVm(model);
        assertNotNull(vm);
        assertEquals("p1", vm.getId());
    }

    @Test
    void paymentProviderMapper_toModel_shouldMapFields() {
        PaymentProviderVm vm = new PaymentProviderVm("p1", "n1", "c1", 1, 1L, "i1");
        PaymentProvider model = paymentProviderMapper.toModel(vm);
        assertNotNull(model);
        assertEquals("p1", model.getId());
    }

    @Test
    void updateMapper_toModel_withNull_shouldReturnNull() {
        assertNull(updateMapper.toModel(null));
    }

    @Test
    void updateMapper_partialUpdate_shouldMapNonNullFields() {
        PaymentProvider model = new PaymentProvider();
        UpdatePaymentVm vm = new UpdatePaymentVm();
        
        updateMapper.partialUpdate(model, null);
        
        vm.setId("p1");
        vm.setName("n1");
        vm.setConfigureUrl("c1");
        vm.setLandingViewComponentName("l1");
        vm.setAdditionalSettings("a1");
        vm.setMediaId(1L);
        
        updateMapper.partialUpdate(model, vm);
        assertEquals("p1", model.getId());
        assertEquals("n1", model.getName());
        assertEquals("c1", model.getConfigureUrl());
        assertEquals("l1", model.getLandingViewComponentName());
        assertEquals("a1", model.getAdditionalSettings());
        assertEquals(1L, model.getMediaId());
    }
    
    @Test
    void updateMapper_toVm_shouldMapFields() {
        PaymentProvider model = PaymentProvider.builder().id("p1").build();
        UpdatePaymentVm vm = updateMapper.toVm(model);
        assertNotNull(vm);
        assertEquals("p1", vm.getId());
        assertNull(updateMapper.toVm(null));
    }

    @Test
    void updateMapper_toVmResponse_shouldMapFields() {
        PaymentProvider model = PaymentProvider.builder().id("p1").build();
        PaymentProviderVm vm = updateMapper.toVmResponse(model);
        assertNotNull(vm);
        assertEquals("p1", vm.getId());
        assertNull(updateMapper.toVmResponse(null));
    }
}
