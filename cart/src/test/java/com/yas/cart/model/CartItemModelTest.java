package com.yas.cart.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartItemModelTest {

    @Test
    @DisplayName("CartItem builder should create with all fields")
    void cartItem_builder_shouldCreateCorrectly() {
        CartItem cartItem = CartItem.builder()
                .customerId("user-1").productId(100L).quantity(5).build();
        assertThat(cartItem.getCustomerId()).isEqualTo("user-1");
        assertThat(cartItem.getProductId()).isEqualTo(100L);
        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("CartItem setters should modify fields")
    void cartItem_setters_shouldWork() {
        CartItem cartItem = new CartItem();
        cartItem.setCustomerId("user-2");
        cartItem.setProductId(200L);
        cartItem.setQuantity(10);
        assertThat(cartItem.getCustomerId()).isEqualTo("user-2");
        assertThat(cartItem.getProductId()).isEqualTo(200L);
        assertThat(cartItem.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("CartItemId equality should work")
    void cartItemId_equality_shouldWork() {
        CartItemId id1 = new CartItemId("user-1", 100L);
        CartItemId id2 = new CartItemId("user-1", 100L);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("CartItemId should not be equal when fields differ")
    void cartItemId_different_shouldNotBeEqual() {
        CartItemId id1 = new CartItemId("user-1", 100L);
        CartItemId id2 = new CartItemId("user-2", 100L);
        CartItemId id3 = new CartItemId("user-1", 200L);
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    @DisplayName("CartItemId getters and setters should work")
    void cartItemId_gettersSetters_shouldWork() {
        CartItemId id = new CartItemId();
        id.setCustomerId("user-x");
        id.setProductId(50L);
        assertThat(id.getCustomerId()).isEqualTo("user-x");
        assertThat(id.getProductId()).isEqualTo(50L);
    }
}
