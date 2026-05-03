package com.yas.order.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void testConstants() {
        assertThat(Constants.ErrorCode.ORDER_NOT_FOUND).isEqualTo("ORDER_NOT_FOUND");
        assertThat(Constants.ErrorCode.CHECKOUT_NOT_FOUND).isEqualTo("CHECKOUT_NOT_FOUND");
        
        assertThat(Constants.MessageCode.CREATE_CHECKOUT).isEqualTo("Create checkout {} by user {}");
        
        assertThat(Constants.Column.ID_COLUMN).isEqualTo("id");
        assertThat(Constants.Column.ORDER_EMAIL_COLUMN).isEqualTo("email");
        assertThat(Constants.Column.ORDER_ITEM_PRODUCT_ID_COLUMN).isEqualTo("productId");
    }
}
