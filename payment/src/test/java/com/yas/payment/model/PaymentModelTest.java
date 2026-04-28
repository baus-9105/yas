package com.yas.payment.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentModelTest {

    @Test
    @DisplayName("Payment entity should correctly store and retrieve all fields")
    void payment_builderAndGetters_shouldWorkCorrectly() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .checkoutId("chk-001")
                .amount(BigDecimal.valueOf(250.50))
                .paymentFee(BigDecimal.valueOf(5.00))
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .gatewayTransactionId("gw-txn-001")
                .failureMessage(null)
                .build();

        assertThat(payment.getId()).isEqualTo(1L);
        assertThat(payment.getOrderId()).isEqualTo(100L);
        assertThat(payment.getCheckoutId()).isEqualTo("chk-001");
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(250.50));
        assertThat(payment.getPaymentFee()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getGatewayTransactionId()).isEqualTo("gw-txn-001");
        assertThat(payment.getFailureMessage()).isNull();
    }

    @Test
    @DisplayName("Payment entity setters should modify fields correctly")
    void payment_setters_shouldModifyFieldsCorrectly() {
        Payment payment = new Payment();
        payment.setId(2L);
        payment.setOrderId(200L);
        payment.setCheckoutId("chk-002");
        payment.setAmount(BigDecimal.valueOf(99.99));
        payment.setPaymentFee(BigDecimal.ZERO);
        payment.setPaymentMethod(PaymentMethod.BANKING);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setGatewayTransactionId("gw-txn-002");
        payment.setFailureMessage("Pending verification");

        assertThat(payment.getId()).isEqualTo(2L);
        assertThat(payment.getOrderId()).isEqualTo(200L);
        assertThat(payment.getCheckoutId()).isEqualTo("chk-002");
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(payment.getPaymentFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.BANKING);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getGatewayTransactionId()).isEqualTo("gw-txn-002");
        assertThat(payment.getFailureMessage()).isEqualTo("Pending verification");
    }

    @Test
    @DisplayName("PaymentProvider entity should correctly store and retrieve all fields")
    void paymentProvider_shouldWorkCorrectly() {
        PaymentProvider provider = PaymentProvider.builder()
                .id("paypal")
                .enabled(true)
                .name("PayPal")
                .configureUrl("https://paypal.com/config")
                .landingViewComponentName("PaypalView")
                .additionalSettings("{\"clientId\": \"abc\"}")
                .mediaId(100L)
                .build();

        assertThat(provider.getId()).isEqualTo("paypal");
        assertThat(provider.isEnabled()).isTrue();
        assertThat(provider.getName()).isEqualTo("PayPal");
        assertThat(provider.getConfigureUrl()).isEqualTo("https://paypal.com/config");
        assertThat(provider.getLandingViewComponentName()).isEqualTo("PaypalView");
        assertThat(provider.getAdditionalSettings()).isEqualTo("{\"clientId\": \"abc\"}");
        assertThat(provider.getMediaId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("PaymentProvider isNew should return false by default")
    void paymentProvider_isNew_shouldReturnFalseByDefault() {
        PaymentProvider provider = new PaymentProvider();
        assertThat(provider.isNew()).isFalse();
    }

    @Test
    @DisplayName("PaymentProvider isNew should return true when set")
    void paymentProvider_isNew_shouldReturnTrueWhenSet() {
        PaymentProvider provider = new PaymentProvider();
        provider.setNew(true);
        assertThat(provider.isNew()).isTrue();
    }

    @Test
    @DisplayName("CapturedPayment builder should work correctly")
    void capturedPayment_builder_shouldWorkCorrectly() {
        CapturedPayment capturedPayment = CapturedPayment.builder()
                .orderId(500L)
                .checkoutId("chk-captured")
                .amount(BigDecimal.valueOf(150))
                .paymentFee(BigDecimal.valueOf(3.50))
                .gatewayTransactionId("gw-captured")
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.COMPLETED)
                .failureMessage(null)
                .build();

        assertThat(capturedPayment.getOrderId()).isEqualTo(500L);
        assertThat(capturedPayment.getCheckoutId()).isEqualTo("chk-captured");
        assertThat(capturedPayment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(capturedPayment.getPaymentFee()).isEqualByComparingTo(BigDecimal.valueOf(3.50));
        assertThat(capturedPayment.getGatewayTransactionId()).isEqualTo("gw-captured");
        assertThat(capturedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(capturedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(capturedPayment.getFailureMessage()).isNull();
    }

    @Test
    @DisplayName("InitiatedPayment builder should work correctly")
    void initiatedPayment_builder_shouldWorkCorrectly() {
        InitiatedPayment initiated = InitiatedPayment.builder()
                .paymentId("pay-init")
                .status("CREATED")
                .redirectUrl("https://paypal.com/pay")
                .build();

        assertThat(initiated.getPaymentId()).isEqualTo("pay-init");
        assertThat(initiated.getStatus()).isEqualTo("CREATED");
        assertThat(initiated.getRedirectUrl()).isEqualTo("https://paypal.com/pay");
    }

    @Test
    @DisplayName("InitiatedPayment setters should modify fields")
    void initiatedPayment_setters_shouldModifyFields() {
        InitiatedPayment initiated = InitiatedPayment.builder()
                .paymentId("old").status("old").redirectUrl("old").build();

        initiated.setPaymentId("new-pay");
        initiated.setStatus("APPROVED");
        initiated.setRedirectUrl("https://new-url.com");

        assertThat(initiated.getPaymentId()).isEqualTo("new-pay");
        assertThat(initiated.getStatus()).isEqualTo("APPROVED");
        assertThat(initiated.getRedirectUrl()).isEqualTo("https://new-url.com");
    }

    @Test
    @DisplayName("CapturedPayment setters should modify fields")
    void capturedPayment_setters_shouldModifyFields() {
        CapturedPayment captured = CapturedPayment.builder()
                .orderId(1L).checkoutId("old").amount(BigDecimal.ONE)
                .paymentFee(BigDecimal.ONE).gatewayTransactionId("old")
                .paymentMethod(PaymentMethod.COD).paymentStatus(PaymentStatus.PENDING)
                .failureMessage(null).build();

        captured.setOrderId(999L);
        captured.setPaymentStatus(PaymentStatus.COMPLETED);
        captured.setFailureMessage("Updated message");

        assertThat(captured.getOrderId()).isEqualTo(999L);
        assertThat(captured.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(captured.getFailureMessage()).isEqualTo("Updated message");
    }
}
