package com.yas.cart.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.cart.model.CartItem;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartItemMapperTest {

    private CartItemMapper cartItemMapper;

    @BeforeEach
    void setUp() {
        cartItemMapper = new CartItemMapper();
    }

    @Test
    @DisplayName("toGetVm should map CartItem to CartItemGetVm correctly")
    void toGetVm_shouldMapAllFields() {
        CartItem cartItem = CartItem.builder()
                .customerId("user-123")
                .productId(10L)
                .quantity(3)
                .build();

        CartItemGetVm result = cartItemMapper.toGetVm(cartItem);

        assertThat(result.customerId()).isEqualTo("user-123");
        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("toCartItem from CartItemPostVm should create CartItem correctly")
    void toCartItem_fromPostVm_shouldCreateCorrectly() {
        CartItemPostVm postVm = CartItemPostVm.builder()
                .productId(5L)
                .quantity(2)
                .build();
        String userId = "user-456";

        CartItem result = cartItemMapper.toCartItem(postVm, userId);

        assertThat(result.getCustomerId()).isEqualTo("user-456");
        assertThat(result.getProductId()).isEqualTo(5L);
        assertThat(result.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("toCartItem from params should create CartItem correctly")
    void toCartItem_fromParams_shouldCreateCorrectly() {
        CartItem result = cartItemMapper.toCartItem("user-789", 20L, 5);

        assertThat(result.getCustomerId()).isEqualTo("user-789");
        assertThat(result.getProductId()).isEqualTo(20L);
        assertThat(result.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("toGetVms should map list of CartItems correctly")
    void toGetVms_shouldMapListCorrectly() {
        CartItem item1 = CartItem.builder().customerId("u1").productId(1L).quantity(1).build();
        CartItem item2 = CartItem.builder().customerId("u1").productId(2L).quantity(3).build();
        CartItem item3 = CartItem.builder().customerId("u1").productId(3L).quantity(5).build();

        List<CartItemGetVm> results = cartItemMapper.toGetVms(List.of(item1, item2, item3));

        assertThat(results).hasSize(3);
        assertThat(results.get(0).productId()).isEqualTo(1L);
        assertThat(results.get(0).quantity()).isEqualTo(1);
        assertThat(results.get(1).productId()).isEqualTo(2L);
        assertThat(results.get(1).quantity()).isEqualTo(3);
        assertThat(results.get(2).productId()).isEqualTo(3L);
        assertThat(results.get(2).quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("toGetVms with empty list should return empty list")
    void toGetVms_emptyList_shouldReturnEmptyList() {
        List<CartItemGetVm> results = cartItemMapper.toGetVms(List.of());

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("toCartItem from PostVm with quantity 1 should work")
    void toCartItem_fromPostVm_withMinQuantity_shouldWork() {
        CartItemPostVm postVm = CartItemPostVm.builder()
                .productId(99L)
                .quantity(1)
                .build();

        CartItem result = cartItemMapper.toCartItem(postVm, "min-user");

        assertThat(result.getQuantity()).isEqualTo(1);
    }
}
