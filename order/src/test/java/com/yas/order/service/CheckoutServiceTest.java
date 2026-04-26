package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.ForbiddenException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.CheckoutMapperImpl;
import com.yas.order.model.Checkout;
import com.yas.order.model.CheckoutItem;
import com.yas.order.model.Order;
import com.yas.order.model.enumeration.CheckoutState;
import com.yas.order.repository.CheckoutItemRepository;
import com.yas.order.repository.CheckoutRepository;
import com.yas.order.viewmodel.checkout.CheckoutPaymentMethodPutVm;
import com.yas.order.viewmodel.checkout.CheckoutPostVm;
import com.yas.order.viewmodel.checkout.CheckoutStatusPutVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.instancio.Instancio;
import static org.instancio.Select.field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CheckoutMapperImpl.class, CheckoutService.class})
class CheckoutServiceTest {

    @MockitoBean
    CheckoutRepository checkoutRepository;

    @MockitoBean
    CheckoutItemRepository checkoutItemRepository;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    ProductService productService;

    @Autowired
    CheckoutService checkoutService;

    CheckoutPostVm checkoutPostVm;
    List<CheckoutItem> checkoutItems;
    Checkout checkoutCreated;
    String checkoutId = UUID.randomUUID().toString();
    List<ProductCheckoutListVm> productCheckoutListVms;
    ProductGetCheckoutListVm productGetCheckoutListVm;
    Map<Long, ProductCheckoutListVm> productCheckoutListVmMap;

    @BeforeEach
    void setUp() {

        checkoutPostVm = Instancio.of(CheckoutPostVm.class)
                .supply(field(CheckoutPostVm.class, "shippingAddressId"), gen -> Long.toString(gen.longRange(1, 10000)))
                .create();
        checkoutCreated = Checkout.builder()
                .id(checkoutId)
                .checkoutState(CheckoutState.PENDING)
                .note(checkoutPostVm.note())
                .email(checkoutPostVm.email())
                .promotionCode(checkoutPostVm.promotionCode())
                .build();
        checkoutCreated.setCreatedBy("test-create-by");
        setSubjectUpSecurityContext(checkoutCreated.getCreatedBy());

        checkoutItems = checkoutPostVm.checkoutItemPostVms().stream()
                .map(itemVm -> CheckoutItem.builder()
                .id(Instancio.create(Long.class))
                .productId(itemVm.productId())
                .quantity(itemVm.quantity())
                .description(itemVm.description())
                .checkout(checkoutCreated)
                .build()
                ).toList();

        productCheckoutListVms = checkoutItems.stream()
                .map(t -> Instancio.of(ProductCheckoutListVm.class)
                        .set(field(ProductCheckoutListVm.class, "id"), t.getProductId())
                        .create())
                .toList();
        productGetCheckoutListVm = new ProductGetCheckoutListVm(
                productCheckoutListVms,
                0,
                productCheckoutListVms.size(),
                productCheckoutListVms.size(),
                1,
                true);
        productCheckoutListVmMap = productCheckoutListVms.stream()
                .collect(Collectors.toMap(ProductCheckoutListVm::getId, Function.identity()));
    }

    @Test
    void testCreateCheckout_whenNormalCase_returnCheckout() {
        checkoutCreated.setCheckoutItems(checkoutItems);
        when(checkoutRepository.save(any())).thenReturn(checkoutCreated);
        when(checkoutItemRepository.saveAll(anyCollection())).thenReturn(checkoutItems);
        when(productService.getProductInfomation(anySet(), anyInt(), anyInt())).thenReturn(productCheckoutListVmMap);
        var res = checkoutService.createCheckout(checkoutPostVm);

        assertThat(res)
                .hasFieldOrPropertyWithValue("id", checkoutId)
                .hasFieldOrPropertyWithValue("email", checkoutPostVm.email())
                .hasFieldOrPropertyWithValue("promotionCode", checkoutPostVm.promotionCode())
                .hasFieldOrPropertyWithValue("note", checkoutPostVm.note());

        assertThat(res.checkoutItemVms())
                .hasSize(checkoutPostVm.checkoutItemPostVms().size())
                .allMatch(item -> item.checkoutId().equals(checkoutId));
    }

    @Test
    void testCreateCheckout_whenCheckoutItemsIsEmpty_throwError() {

        when(checkoutRepository.save(any())).thenReturn(checkoutCreated);
        when(checkoutItemRepository.saveAll(anyCollection())).thenReturn(List.of());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> checkoutService.createCheckout(checkoutPostVm));
        assertThat(exception).hasMessage("PRODUCT_NOT_FOUND");
    }

    @Test
    void testGetCheckoutPendingStateWithItemsById_whenNormalCase_returnCheckoutVm() {
        checkoutCreated.setCheckoutItems(checkoutItems);
        when(checkoutRepository.findByIdAndCheckoutState(anyString(), eq(CheckoutState.PENDING)))
                .thenReturn(Optional.ofNullable(checkoutCreated));
        when(checkoutItemRepository.findAllByCheckoutId(anyString())).thenReturn(checkoutItems);

        var res = checkoutService.getCheckoutPendingStateWithItemsById("1");

        assertThat(res)
                .hasFieldOrPropertyWithValue("id", checkoutId)
                .hasFieldOrPropertyWithValue("promotionCode", checkoutPostVm.promotionCode())
                .hasFieldOrPropertyWithValue("email", checkoutPostVm.email())
                .hasFieldOrPropertyWithValue("note", checkoutPostVm.note());

        assertThat(res.checkoutItemVms())
                .allMatch(item -> item.checkoutId().equals(checkoutId))
                .hasSize(checkoutPostVm.checkoutItemPostVms().size());
    }

    @Test
    void testGetCheckoutPendingStateWithItemsById_whenNotEqualsCreateBy_throwForbidden() {

        when(checkoutRepository.findByIdAndCheckoutState(anyString(), eq(CheckoutState.PENDING)))
                .thenReturn(Optional.ofNullable(checkoutCreated));
        setSubjectUpSecurityContext("test--by");

        Assertions.assertThrows(ForbiddenException.class,
                () -> checkoutService.getCheckoutPendingStateWithItemsById("1"),
                "You don't have permission to access this page");

    }

    @Test
    void testGetCheckoutPendingStateWithItemsById_whenNormalCase_returnCheckoutVmWithoutCheckoutItems() {
        when(checkoutRepository.findByIdAndCheckoutState(anyString(), eq(CheckoutState.PENDING)))
                .thenReturn(Optional.ofNullable(checkoutCreated));
        when(checkoutItemRepository.findAllByCheckoutId(anyString())).thenReturn(List.of());

        var res = checkoutService.getCheckoutPendingStateWithItemsById("1");

        assertThat(res)
                .hasFieldOrPropertyWithValue("id", checkoutId)
                .hasFieldOrPropertyWithValue("promotionCode", checkoutPostVm.promotionCode())
                .hasFieldOrPropertyWithValue("note", checkoutPostVm.note())
                .hasFieldOrPropertyWithValue("email", checkoutPostVm.email());

        assertThat(res.checkoutItemVms()).isNull();
    }

    @Test
    void testGetCheckoutPendingStateWithItemsById_whenCheckoutNotFound_thenThrowNotFoundException() {
        when(checkoutRepository.findByIdAndCheckoutState(anyString(), eq(CheckoutState.PENDING)))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> checkoutService.getCheckoutPendingStateWithItemsById("missing"));
    }

    @Test
    void testUpdateCheckoutStatus_whenNormalCase_thenReturnOrderId() {
        Checkout checkout = Checkout.builder()
            .id(checkoutId)
            .checkoutState(CheckoutState.PENDING)
            .build();
        checkout.setCreatedBy(checkoutCreated.getCreatedBy());
        Order order = new Order();
        order.setId(99L);

        when(checkoutRepository.findById(checkoutId)).thenReturn(Optional.of(checkout));
        when(checkoutRepository.save(checkout)).thenReturn(checkout);
        when(orderService.findOrderByCheckoutId(checkoutId)).thenReturn(order);

        Long result = checkoutService.updateCheckoutStatus(
            new CheckoutStatusPutVm(checkoutId, CheckoutState.COMPLETED.name()));

        assertThat(result).isEqualTo(99L);
        assertThat(checkout.getCheckoutState()).isEqualTo(CheckoutState.COMPLETED);
    }

    @Test
    void testUpdateCheckoutStatus_whenCheckoutNotFound_thenThrowNotFoundException() {
        when(checkoutRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> checkoutService.updateCheckoutStatus(new CheckoutStatusPutVm("missing", CheckoutState.COMPLETED.name())));
    }

    @Test
    void testUpdateCheckoutStatus_whenNotOwnedByCurrentUser_thenThrowForbiddenException() {
        Checkout checkout = Checkout.builder()
            .id(checkoutId)
            .checkoutState(CheckoutState.PENDING)
            .build();
        checkout.setCreatedBy("other-user");
        when(checkoutRepository.findById(checkoutId)).thenReturn(Optional.of(checkout));
        setSubjectUpSecurityContext("current-user");

        assertThrows(ForbiddenException.class,
            () -> checkoutService.updateCheckoutStatus(new CheckoutStatusPutVm(checkoutId, CheckoutState.COMPLETED.name())));
    }

    @Test
    void testUpdateCheckoutPaymentMethod_whenCheckoutExists_thenUpdatePaymentMethod() {
        // Arrange
        String id = "123";
        Checkout checkout = new Checkout();
        checkout.setId(id);

        CheckoutPaymentMethodPutVm request = new CheckoutPaymentMethodPutVm("new-payment-method-id");

        when(checkoutRepository.findById(id)).thenReturn(Optional.of(checkout));

        // Act
        checkoutService.updateCheckoutPaymentMethod(id, request);

        // Assert
        verify(checkoutRepository).save(checkout);
        assertThat(checkout.getPaymentMethodId()).isEqualTo(request.paymentMethodId());
    }

    @Test
    void testUpdateCheckoutPaymentMethod_whenCheckoutNotFound_thenThrowNotFoundException() {
        // Arrange
        String id = "invalid-id";
        CheckoutPaymentMethodPutVm request = new CheckoutPaymentMethodPutVm("new-payment-method-id");

        when(checkoutRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> checkoutService.updateCheckoutPaymentMethod(id, request));
    }

    @Test
    void testUpdateCheckoutPaymentMethod_whenPaymentMethodIdIsNull_thenDoNotUpdate() {
        // Arrange
        String id = "123";
        Checkout checkout = new Checkout();
        checkout.setId(id);

        CheckoutPaymentMethodPutVm request = new CheckoutPaymentMethodPutVm(null);

        when(checkoutRepository.findById(id)).thenReturn(Optional.of(checkout));

        // Act
        checkoutService.updateCheckoutPaymentMethod(id, request);

        // Assert
        verify(checkoutRepository).save(checkout);
        assertThat(checkout.getPaymentMethodId()).isNull();
    }
}