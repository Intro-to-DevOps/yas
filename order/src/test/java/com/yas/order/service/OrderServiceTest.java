package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
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
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

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
        Order order = createOrder(1L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.getLatestOrders(PageRequest.of(0, 1))).thenReturn(List.of(order));

        assertThat(orderService.getLatestOrders(1)).hasSize(1);
    }

    @Test
    void getAllOrder_whenNoResult_shouldReturnZeroMetadata() {
        when(orderRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10,
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
    void getAllOrder_whenHasData_shouldReturnMappedOrderBriefs() {
        Order order = createOrder(1L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10,
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
        Order order = createOrder(1L, OrderStatus.PENDING, PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(1L)
            .paymentId(100L)
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .build();

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(request);

        assertEquals(OrderStatus.PAID, order.getOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(100L, result.paymentId());
    }

    @Test
    void updateOrderPaymentStatus_whenCancelled_shouldNotChangeOrderStatusToPaid() {
        Order order = createOrder(1L, OrderStatus.ACCEPTED, PaymentStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(1L)
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
        Order order = createOrder(1L, OrderStatus.PENDING, PaymentStatus.PENDING);
        order.setCheckoutId("checkout-1");
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));

        Order result = orderService.findOrderByCheckoutId("checkout-1");

        assertEquals(1L, result.getId());
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
}


