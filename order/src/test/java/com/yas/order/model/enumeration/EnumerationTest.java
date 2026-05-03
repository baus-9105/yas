package com.yas.order.model.enumeration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnumerationTest {

    @Test
    @DisplayName("CheckoutState should have correct values")
    void checkoutState_shouldHaveCorrectValues() {
        CheckoutState[] states = CheckoutState.values();
        assertThat(states).hasSize(8);
        assertThat(states).containsExactly(
            CheckoutState.COMPLETED, CheckoutState.PENDING, CheckoutState.LOCK,
            CheckoutState.CHECKED_OUT, CheckoutState.PAYMENT_PROCESSING, CheckoutState.PAYMENT_FAILED,
            CheckoutState.PAYMENT_CONFIRMED, CheckoutState.FULFILLED
        );
    }

    @Test
    @DisplayName("DeliveryMethod should have correct values")
    void deliveryMethod_shouldHaveCorrectValues() {
        DeliveryMethod[] methods = DeliveryMethod.values();
        assertThat(methods).hasSize(4);
        assertThat(methods).containsExactly(
            DeliveryMethod.VIETTEL_POST, DeliveryMethod.GRAB_EXPRESS, 
            DeliveryMethod.SHOPEE_EXPRESS, DeliveryMethod.YAS_EXPRESS
        );
    }

    @Test
    @DisplayName("DeliveryStatus should have correct values")
    void deliveryStatus_shouldHaveCorrectValues() {
        DeliveryStatus[] statuses = DeliveryStatus.values();
        assertThat(statuses).hasSize(4);
        assertThat(statuses).containsExactly(DeliveryStatus.PREPARING, DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED);
    }

    @Test
    @DisplayName("OrderStatus should have correct values and getName")
    void orderStatus_shouldHaveCorrectValuesAndGetName() {
        OrderStatus[] statuses = OrderStatus.values();
        assertThat(statuses).hasSize(9);
        assertThat(statuses).containsExactly(
                OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.COMPLETED,
                OrderStatus.REFUND, OrderStatus.CANCELLED, OrderStatus.REJECT
        );

        assertThat(OrderStatus.PENDING.getName()).isEqualTo("PENDING");
        assertThat(OrderStatus.ACCEPTED.getName()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("PaymentMethod should have correct values")
    void paymentMethod_shouldHaveCorrectValues() {
        PaymentMethod[] methods = PaymentMethod.values();
        assertThat(methods).hasSize(3);
        assertThat(methods).containsExactly(PaymentMethod.COD, PaymentMethod.BANKING, PaymentMethod.PAYPAL);
    }

    @Test
    @DisplayName("PaymentStatus should have correct values")
    void paymentStatus_shouldHaveCorrectValues() {
        PaymentStatus[] statuses = PaymentStatus.values();
        assertThat(statuses).hasSize(3);
        assertThat(statuses).containsExactly(PaymentStatus.PENDING, PaymentStatus.COMPLETED, PaymentStatus.CANCELLED);
    }
}
