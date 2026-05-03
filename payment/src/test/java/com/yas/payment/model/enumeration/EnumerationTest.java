package com.yas.payment.model.enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnumerationTest {

    @Test
    @DisplayName("PaymentMethod should have COD, BANKING, PAYPAL values")
    void paymentMethod_shouldHaveCorrectValues() {
        PaymentMethod[] methods = PaymentMethod.values();
        assertThat(methods).hasSize(3);
        assertThat(methods).containsExactly(PaymentMethod.COD, PaymentMethod.BANKING, PaymentMethod.PAYPAL);
    }

    @Test
    @DisplayName("PaymentMethod valueOf should return correct enum")
    void paymentMethod_valueOf_shouldReturnCorrectEnum() {
        assertEquals(PaymentMethod.COD, PaymentMethod.valueOf("COD"));
        assertEquals(PaymentMethod.BANKING, PaymentMethod.valueOf("BANKING"));
        assertEquals(PaymentMethod.PAYPAL, PaymentMethod.valueOf("PAYPAL"));
    }

    @Test
    @DisplayName("PaymentMethod valueOf with invalid name should throw exception")
    void paymentMethod_valueOf_withInvalidName_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.valueOf("INVALID"));
    }

    @Test
    @DisplayName("PaymentStatus should have PENDING, COMPLETED, CANCELLED values")
    void paymentStatus_shouldHaveCorrectValues() {
        PaymentStatus[] statuses = PaymentStatus.values();
        assertThat(statuses).hasSize(3);
        assertThat(statuses).containsExactly(PaymentStatus.PENDING, PaymentStatus.COMPLETED, PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("PaymentStatus valueOf should return correct enum")
    void paymentStatus_valueOf_shouldReturnCorrectEnum() {
        assertEquals(PaymentStatus.PENDING, PaymentStatus.valueOf("PENDING"));
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.valueOf("COMPLETED"));
        assertEquals(PaymentStatus.CANCELLED, PaymentStatus.valueOf("CANCELLED"));
    }

    @Test
    @DisplayName("PaymentStatus valueOf with invalid name should throw exception")
    void paymentStatus_valueOf_withInvalidName_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.valueOf("INVALID"));
    }

    @Test
    @DisplayName("PaymentMethod name should return string representation")
    void paymentMethod_name_shouldReturnStringRepresentation() {
        assertThat(PaymentMethod.PAYPAL.name()).isEqualTo("PAYPAL");
        assertThat(PaymentMethod.COD.name()).isEqualTo("COD");
        assertThat(PaymentMethod.BANKING.name()).isEqualTo("BANKING");
    }

    @Test
    @DisplayName("PaymentStatus name should return string representation")
    void paymentStatus_name_shouldReturnStringRepresentation() {
        assertThat(PaymentStatus.PENDING.name()).isEqualTo("PENDING");
        assertThat(PaymentStatus.COMPLETED.name()).isEqualTo("COMPLETED");
        assertThat(PaymentStatus.CANCELLED.name()).isEqualTo("CANCELLED");
    }
}
