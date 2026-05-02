package com.yas.cart.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ViewModelTest {

    @Test
    @DisplayName("CartItemGetVm builder should work")
    void cartItemGetVm_builder() {
        CartItemGetVm vm = CartItemGetVm.builder()
                .customerId("u1").productId(1L).quantity(3).build();
        assertThat(vm.customerId()).isEqualTo("u1");
        assertThat(vm.productId()).isEqualTo(1L);
        assertThat(vm.quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("CartItemPostVm builder should work")
    void cartItemPostVm_builder() {
        CartItemPostVm vm = CartItemPostVm.builder()
                .productId(5L).quantity(2).build();
        assertThat(vm.productId()).isEqualTo(5L);
        assertThat(vm.quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("CartItemPutVm should store quantity")
    void cartItemPutVm_shouldWork() {
        CartItemPutVm vm = new CartItemPutVm(10);
        assertThat(vm.quantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("CartItemDeleteVm should store productId and quantity")
    void cartItemDeleteVm_shouldWork() {
        CartItemDeleteVm vm = new CartItemDeleteVm(7L, 3);
        assertThat(vm.productId()).isEqualTo(7L);
        assertThat(vm.quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("ProductThumbnailVm builder should work")
    void productThumbnailVm_shouldWork() {
        ProductThumbnailVm vm = new ProductThumbnailVm(1L, "Phone", "phone", "http://img.com/phone.jpg");
        assertThat(vm.id()).isEqualTo(1L);
        assertThat(vm.name()).isEqualTo("Phone");
        assertThat(vm.slug()).isEqualTo("phone");
        assertThat(vm.thumbnailUrl()).isEqualTo("http://img.com/phone.jpg");
    }

    @Test
    @DisplayName("CartItemDeleteVm builder should work")
    void cartItemDeleteVm_builder() {
        CartItemDeleteVm vm = CartItemDeleteVm.builder()
                .productId(10L).quantity(5).build();
        assertThat(vm.productId()).isEqualTo(10L);
        assertThat(vm.quantity()).isEqualTo(5);
    }
}
