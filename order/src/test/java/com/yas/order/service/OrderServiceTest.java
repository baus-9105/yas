package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private ProductService productService;
    private CartService cartService;
    private OrderMapper orderMapper;
    private PromotionService promotionService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        productService = mock(ProductService.class);
        cartService = mock(CartService.class);
        orderMapper = mock(OrderMapper.class);
        promotionService = mock(PromotionService.class);

        orderService = new OrderService(orderRepository, orderItemRepository, productService, cartService, orderMapper, promotionService);
    }

    @Test
    @DisplayName("acceptOrder should update status to ACCEPTED")
    void acceptOrder_shouldUpdateStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.PENDING);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(1L);

        verify(orderRepository).save(order);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("acceptOrder should throw NotFoundException if order not found")
    void acceptOrder_whenOrderNotFound_shouldThrowException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.acceptOrder(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("rejectOrder should update status to REJECT with reason")
    void rejectOrder_shouldUpdateStatusAndReason() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.PENDING);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(1L, "Out of stock");

        verify(orderRepository).save(order);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("Out of stock");
    }

    @Test
    @DisplayName("findOrderByCheckoutId should return order if found")
    void findOrderByCheckoutId_shouldReturnOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setCheckoutId("chk-123");
        
        when(orderRepository.findByCheckoutId("chk-123")).thenReturn(Optional.of(order));

        Order result = orderService.findOrderByCheckoutId("chk-123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateOrderPaymentStatus should update payment details")
    void updateOrderPaymentStatus_shouldUpdateDetails() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(1L)
                .paymentId(100L)
                .paymentStatus("COMPLETED")
                .build();

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(vm);

        verify(orderRepository).save(order);
        assertThat(order.getPaymentId()).isEqualTo(100L);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        
        assertThat(result.orderStatus()).isEqualTo("PAID");
        assertThat(result.paymentStatus()).isEqualTo("COMPLETED");
    }
}
