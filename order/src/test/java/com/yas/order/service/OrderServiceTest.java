package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.nullable;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    @SuppressWarnings("unused")
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_whenRequestIsValid_shouldPersistOrderAndTriggerSideEffects() {
        OrderPostVm request = createOrderPostVm();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderRepository.findById(1L)).thenReturn(Optional.of(createOrder(1L, OrderStatus.PENDING, PaymentStatus.PENDING)));
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());

        var result = orderService.createOrder(request);

        assertEquals(1L, result.id());
        assertEquals(request.checkoutId(), result.checkoutId());
        verify(productService).subtractProductStockQuantity(eq(result));
        verify(cartService).deleteCartItems(eq(result));
        verify(promotionService).updateUsagePromotion(anyList());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void getLatestOrders_whenCountLessOrEqualZero_shouldReturnEmpty() {
        assertThat(orderService.getLatestOrders(0)).isEmpty();
    }

    @Test
    void getLatestOrders_whenRepositoryReturnsEmpty_shouldReturnEmpty() {
        when(orderRepository.getLatestOrders(PageRequest.of(0, 5))).thenReturn(List.of());

        assertThat(orderService.getLatestOrders(5)).isEmpty();
    }

    @Test
    void getLatestOrders_whenRepositoryHasData_shouldReturnMappedBriefList() {
        Order order = createOrder(2L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.getLatestOrders(PageRequest.of(0, 1))).thenReturn(List.of(order));

        assertThat(orderService.getLatestOrders(1)).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_whenNoResult_shouldReturnZeroMetadata() {
        when(orderRepository.findAll(nullable(Specification.class), eq(PageRequest.of(0, 10,
            Sort.by(Sort.Direction.DESC, "createdOn")))))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        OrderListVm result = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(30), ZonedDateTime.now()),
            null,
            List.of(),
            Pair.of("", ""),
            null,
            Pair.of(0, 10)
        );

        assertThat(result.orderList()).isNull();
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllOrder_whenHasData_shouldReturnMappedOrderBriefs() {
        Order order = createOrder(3L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.findAll(nullable(Specification.class), eq(PageRequest.of(0, 10,
            Sort.by(Sort.Direction.DESC, "createdOn")))))
            .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));

        OrderListVm result = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "sku",
            List.of(OrderStatus.PENDING),
            Pair.of("VN", "0123"),
            "a@b.com",
            Pair.of(0, 10)
        );

        assertThat(result.orderList()).hasSize(1);
        assertEquals(1, result.totalElements());
    }

    @Test
    void updateOrderPaymentStatus_whenCompleted_shouldSetOrderPaid() {
        Order order = createOrder(4L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(4L)
            .paymentId(100L)
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .build();

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(request);

        assertEquals(OrderStatus.PAID, order.getOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(100L, result.paymentId());
    }

    @Test
    void updateOrderPaymentStatus_whenOrderNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(999L)
            .paymentId(100L)
            .paymentStatus(PaymentStatus.CANCELLED.name())
            .build();

        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(request));
    }

    @Test
    void updateOrderPaymentStatus_whenCancelled_shouldNotChangeOrderStatusToPaid() {
        Order order = createOrder(5L, OrderStatus.ACCEPTED, PaymentStatus.PENDING);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(5L)
            .paymentId(100L)
            .paymentStatus(PaymentStatus.CANCELLED.name())
            .build();

        orderService.updateOrderPaymentStatus(request);

        assertEquals(OrderStatus.ACCEPTED, order.getOrderStatus());
        assertEquals(PaymentStatus.CANCELLED, order.getPaymentStatus());
    }

    @Test
    void findOrderByCheckoutId_whenMissing_shouldThrowNotFoundException() {
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId("checkout-1"));
    }

    @Test
    void findOrderByCheckoutId_whenFound_shouldReturnOrder() {
        Order order = createOrder(6L, OrderStatus.PENDING, PaymentStatus.PENDING);
        order.setCheckoutId("checkout-1");
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));

        Order result = orderService.findOrderByCheckoutId("checkout-1");

        assertEquals(6L, result.getId());
    }

    @Test
    void findOrderVmByCheckoutId_whenOrderFound_shouldReturnVmWithItems() {
        Order order = createOrder(7L, OrderStatus.PENDING, PaymentStatus.PENDING);
        order.setCheckoutId("checkout-1");
        OrderItem orderItem = OrderItem.builder()
            .id(10L)
            .orderId(7L)
            .productId(20L)
            .productName("Product A")
            .quantity(2)
            .productPrice(BigDecimal.TEN)
            .build();
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(7L)).thenReturn(List.of(orderItem));

        var result = orderService.findOrderVmByCheckoutId("checkout-1");

        assertEquals(7L, result.id());
        assertThat(result.orderItems()).hasSize(1);
    }

    @Test
    void getOrderWithItemsById_whenOrderFound_shouldReturnVmWithItems() {
        Order order = createOrder(8L, OrderStatus.PENDING, PaymentStatus.PENDING);
        OrderItem orderItem = OrderItem.builder()
            .id(10L)
            .orderId(8L)
            .productId(20L)
            .productName("Product A")
            .quantity(2)
            .productPrice(BigDecimal.TEN)
            .build();
        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(8L)).thenReturn(List.of(orderItem));

        var result = orderService.getOrderWithItemsById(8L);

        assertEquals(8L, result.id());
        assertThat(result.orderItemVms()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void isOrderCompletedWithUserIdAndProductId_whenProductHasNoVariations_shouldReturnTrueIfOrderExists() {
        mockCurrentUserId();
        when(productService.getProductVariations(1L)).thenReturn(List.of());
        when(orderRepository.findOne(nullable(Specification.class)))
            .thenReturn(Optional.of(createOrder(9L, OrderStatus.ACCEPTED, PaymentStatus.COMPLETED)));

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(1L);

        assertThat(result.isPresent()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void isOrderCompletedWithUserIdAndProductId_whenProductHasVariations_shouldReturnFalseIfOrderMissing() {
        mockCurrentUserId();
        when(productService.getProductVariations(1L)).thenReturn(List.of(
            new ProductVariationVm(11L, "Variant 1", "SKU-11"),
            new ProductVariationVm(12L, "Variant 2", "SKU-12")
        ));
        when(orderRepository.findOne(nullable(Specification.class)))
            .thenReturn(Optional.empty());

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(1L);

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyOrders_whenOrdersExist_shouldReturnMappedOrders() {
        mockCurrentUserId();
        Order order = createOrder(10L, OrderStatus.ACCEPTED, PaymentStatus.COMPLETED);
        when(orderRepository.findAll(nullable(Specification.class), any(Sort.class)))
            .thenReturn(List.of(order));

        List<OrderGetVm> result = orderService.getMyOrders("product", OrderStatus.ACCEPTED);

        assertThat(result).hasSize(1);
        assertEquals(10L, result.getFirst().id());
    }

    @Test
    void rejectAndAcceptOrder_shouldUpdateStatusAndPersist() {
        Order order = createOrder(1L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.rejectOrder(1L, "invalid payment");
        assertEquals(OrderStatus.REJECT, order.getOrderStatus());
        assertEquals("invalid payment", order.getRejectReason());

        orderService.acceptOrder(1L);
        assertEquals(OrderStatus.ACCEPTED, order.getOrderStatus());
        verify(orderRepository, times(2)).save(order);
    }

    private Order createOrder(Long id, OrderStatus orderStatus, PaymentStatus paymentStatus) {
        OrderAddress orderAddress = OrderAddress.builder()
            .id(1L)
            .contactName("Tester")
            .phone("0123")
            .addressLine1("line1")
            .city("HCM")
            .countryId(1L)
            .countryName("VN")
            .build();

        return Order.builder()
            .id(id)
            .email("a@b.com")
            .billingAddressId(orderAddress)
            .shippingAddressId(orderAddress)
            .totalPrice(BigDecimal.TEN)
            .orderStatus(orderStatus)
            .paymentStatus(paymentStatus)
            .build();
    }

    private OrderPostVm createOrderPostVm() {
        OrderAddressPostVm shippingAddress = OrderAddressPostVm.builder()
            .contactName("Ship To")
            .phone("0900000000")
            .addressLine1("Shipping line 1")
            .addressLine2("Shipping line 2")
            .city("HCM")
            .zipCode("700000")
            .districtId(1L)
            .districtName("District 1")
            .stateOrProvinceId(1L)
            .stateOrProvinceName("HCM")
            .countryId(1L)
            .countryName("VN")
            .build();

        OrderAddressPostVm billingAddress = OrderAddressPostVm.builder()
            .contactName("Bill To")
            .phone("0900000001")
            .addressLine1("Billing line 1")
            .addressLine2("Billing line 2")
            .city("HCM")
            .zipCode("700001")
            .districtId(2L)
            .districtName("District 2")
            .stateOrProvinceId(1L)
            .stateOrProvinceName("HCM")
            .countryId(1L)
            .countryName("VN")
            .build();

        OrderItemPostVm orderItem = OrderItemPostVm.builder()
            .productId(11L)
            .productName("Product A")
            .quantity(2)
            .productPrice(BigDecimal.TEN)
            .note("note")
            .build();

        return OrderPostVm.builder()
            .checkoutId("checkout-1")
            .email("customer@example.com")
            .shippingAddressPostVm(shippingAddress)
            .billingAddressPostVm(billingAddress)
            .note("order note")
            .tax(1.5f)
            .discount(2.5f)
            .numberItem(1)
            .totalPrice(BigDecimal.valueOf(100))
            .deliveryFee(BigDecimal.valueOf(15))
            .couponCode("PROMO10")
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .paymentMethod(PaymentMethod.COD)
            .paymentStatus(PaymentStatus.PENDING)
            .orderItemPostVms(List.of(orderItem))
            .build();
    }

    private void mockCurrentUserId() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("userId");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}


