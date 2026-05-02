package com.yas.payment.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.MediaVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ViewModelTest {

    @Nested
    @DisplayName("InitPaymentRequestVm tests")
    class InitPaymentRequestVmTests {

        @Test
        @DisplayName("Should create via builder and access fields")
        void initPaymentRequestVm_shouldCreateCorrectly() {
            InitPaymentRequestVm vm = InitPaymentRequestVm.builder()
                    .paymentMethod("PAYPAL")
                    .totalPrice(BigDecimal.valueOf(100))
                    .checkoutId("chk-1")
                    .build();

            assertThat(vm.paymentMethod()).isEqualTo("PAYPAL");
            assertThat(vm.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(vm.checkoutId()).isEqualTo("chk-1");
        }
    }

    @Nested
    @DisplayName("InitPaymentResponseVm tests")
    class InitPaymentResponseVmTests {

        @Test
        @DisplayName("Should create via builder and access fields")
        void initPaymentResponseVm_shouldCreateCorrectly() {
            InitPaymentResponseVm vm = InitPaymentResponseVm.builder()
                    .status("CREATED")
                    .paymentId("pay-1")
                    .redirectUrl("https://paypal.com")
                    .build();

            assertThat(vm.status()).isEqualTo("CREATED");
            assertThat(vm.paymentId()).isEqualTo("pay-1");
            assertThat(vm.redirectUrl()).isEqualTo("https://paypal.com");
        }

        @Test
        @DisplayName("Should handle null redirect URL")
        void initPaymentResponseVm_withNullRedirectUrl_shouldWork() {
            InitPaymentResponseVm vm = InitPaymentResponseVm.builder()
                    .status("CREATED")
                    .paymentId("pay-2")
                    .redirectUrl(null)
                    .build();

            assertThat(vm.redirectUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("CapturePaymentRequestVm tests")
    class CapturePaymentRequestVmTests {

        @Test
        @DisplayName("Should create via builder and access fields")
        void capturePaymentRequestVm_shouldCreateCorrectly() {
            CapturePaymentRequestVm vm = CapturePaymentRequestVm.builder()
                    .paymentMethod("PAYPAL")
                    .token("token-123")
                    .build();

            assertThat(vm.paymentMethod()).isEqualTo("PAYPAL");
            assertThat(vm.token()).isEqualTo("token-123");
        }
    }

    @Nested
    @DisplayName("CapturePaymentResponseVm tests")
    class CapturePaymentResponseVmTests {

        @Test
        @DisplayName("Should create via builder and access all fields")
        void capturePaymentResponseVm_shouldCreateCorrectly() {
            CapturePaymentResponseVm vm = CapturePaymentResponseVm.builder()
                    .orderId(1L)
                    .checkoutId("chk-1")
                    .amount(BigDecimal.valueOf(100))
                    .paymentFee(BigDecimal.valueOf(2.50))
                    .gatewayTransactionId("gw-1")
                    .paymentMethod(PaymentMethod.PAYPAL)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .failureMessage(null)
                    .build();

            assertThat(vm.orderId()).isEqualTo(1L);
            assertThat(vm.checkoutId()).isEqualTo("chk-1");
            assertThat(vm.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(vm.paymentFee()).isEqualByComparingTo(BigDecimal.valueOf(2.50));
            assertThat(vm.gatewayTransactionId()).isEqualTo("gw-1");
            assertThat(vm.paymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
            assertThat(vm.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(vm.failureMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("PaymentOrderStatusVm tests")
    class PaymentOrderStatusVmTests {

        @Test
        @DisplayName("Should create via builder and access all fields")
        void paymentOrderStatusVm_shouldCreateCorrectly() {
            PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                    .orderId(10L)
                    .orderStatus("COMPLETED")
                    .paymentId(20L)
                    .paymentStatus("SUCCESS")
                    .build();

            assertThat(vm.orderId()).isEqualTo(10L);
            assertThat(vm.orderStatus()).isEqualTo("COMPLETED");
            assertThat(vm.paymentId()).isEqualTo(20L);
            assertThat(vm.paymentStatus()).isEqualTo("SUCCESS");
        }
    }

    @Nested
    @DisplayName("CheckoutStatusVm tests")
    class CheckoutStatusVmTests {

        @Test
        @DisplayName("Should create and access fields")
        void checkoutStatusVm_shouldCreateCorrectly() {
            CheckoutStatusVm vm = new CheckoutStatusVm("chk-1", "COMPLETED");

            assertThat(vm.checkoutId()).isEqualTo("chk-1");
            assertThat(vm.checkoutStatus()).isEqualTo("COMPLETED");
        }
    }

    @Nested
    @DisplayName("PaymentProviderVm tests")
    class PaymentProviderVmTests {

        @Test
        @DisplayName("Should create via constructor and access all fields")
        void paymentProviderVm_shouldWorkCorrectly() {
            PaymentProviderVm vm = new PaymentProviderVm(
                    "paypal", "PayPal", "https://paypal.com", 1, 100L, "https://icon.com/paypal.png"
            );

            assertThat(vm.getId()).isEqualTo("paypal");
            assertThat(vm.getName()).isEqualTo("PayPal");
            assertThat(vm.getConfigureUrl()).isEqualTo("https://paypal.com");
            assertThat(vm.getVersion()).isEqualTo(1);
            assertThat(vm.getMediaId()).isEqualTo(100L);
            assertThat(vm.getIconUrl()).isEqualTo("https://icon.com/paypal.png");
        }

        @Test
        @DisplayName("Should allow setting iconUrl")
        void paymentProviderVm_setIconUrl_shouldWork() {
            PaymentProviderVm vm = new PaymentProviderVm(
                    "cod", "COD", "https://cod.com", 1, null, null
            );
            vm.setIconUrl("https://icon.com/cod.png");
            assertThat(vm.getIconUrl()).isEqualTo("https://icon.com/cod.png");
        }
    }

    @Nested
    @DisplayName("MediaVm tests")
    class MediaVmTests {

        @Test
        @DisplayName("Should create via builder and access all fields")
        void mediaVm_builder_shouldWorkCorrectly() {
            MediaVm vm = MediaVm.builder()
                    .id(1L)
                    .caption("PayPal Icon")
                    .fileName("paypal.png")
                    .mediaType("image/png")
                    .url("https://media.com/paypal.png")
                    .build();

            assertThat(vm.getId()).isEqualTo(1L);
            assertThat(vm.getCaption()).isEqualTo("PayPal Icon");
            assertThat(vm.getFileName()).isEqualTo("paypal.png");
            assertThat(vm.getMediaType()).isEqualTo("image/png");
            assertThat(vm.getUrl()).isEqualTo("https://media.com/paypal.png");
        }

        @Test
        @DisplayName("Should create default MediaVm with builder")
        void mediaVm_emptyBuilder_shouldReturnEmptyVm() {
            MediaVm vm = MediaVm.builder().build();

            assertThat(vm.getId()).isNull();
            assertThat(vm.getUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("PaymentProviderReqVm (Create/Update) tests")
    class PaymentProviderReqVmTests {

        @Test
        @DisplayName("CreatePaymentVm should inherit and work correctly")
        void createPaymentVm_shouldWorkCorrectly() {
            CreatePaymentVm vm = new CreatePaymentVm();
            vm.setId("paypal");
            vm.setEnabled(true);
            vm.setName("PayPal");
            vm.setConfigureUrl("https://paypal.com/config");
            vm.setLandingViewComponentName("PaypalView");
            vm.setAdditionalSettings("{\"key\": \"value\"}");
            vm.setMediaId(100L);

            assertThat(vm.getId()).isEqualTo("paypal");
            assertThat(vm.isEnabled()).isTrue();
            assertThat(vm.getName()).isEqualTo("PayPal");
            assertThat(vm.getConfigureUrl()).isEqualTo("https://paypal.com/config");
            assertThat(vm.getLandingViewComponentName()).isEqualTo("PaypalView");
            assertThat(vm.getAdditionalSettings()).isEqualTo("{\"key\": \"value\"}");
            assertThat(vm.getMediaId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("UpdatePaymentVm should inherit and work correctly")
        void updatePaymentVm_shouldWorkCorrectly() {
            UpdatePaymentVm vm = new UpdatePaymentVm();
            vm.setId("banking");
            vm.setEnabled(false);
            vm.setName("Banking");
            vm.setConfigureUrl("https://bank.com/config");

            assertThat(vm.getId()).isEqualTo("banking");
            assertThat(vm.isEnabled()).isFalse();
            assertThat(vm.getName()).isEqualTo("Banking");
            assertThat(vm.getConfigureUrl()).isEqualTo("https://bank.com/config");
        }

        @Test
        @DisplayName("PaymentProviderReqVm default values should be null/false")
        void paymentProviderReqVm_defaultValues_shouldBeNullOrFalse() {
            CreatePaymentVm vm = new CreatePaymentVm();

            assertThat(vm.getId()).isNull();
            assertThat(vm.isEnabled()).isFalse();
            assertThat(vm.getName()).isNull();
            assertThat(vm.getConfigureUrl()).isNull();
            assertThat(vm.getLandingViewComponentName()).isNull();
            assertThat(vm.getAdditionalSettings()).isNull();
            assertThat(vm.getMediaId()).isNull();
        }
    }
    @Nested
    @DisplayName("ErrorVm tests")
    class ErrorVmTests {

        @Test
        @DisplayName("Should create ErrorVm with statusCode, title, detail, and empty fieldErrors")
        void errorVm_constructor_shouldCreateCorrectly() {
            ErrorVm vm = new ErrorVm("404", "Not Found", "Resource not found");

            assertThat(vm.statusCode()).isEqualTo("404");
            assertThat(vm.title()).isEqualTo("Not Found");
            assertThat(vm.detail()).isEqualTo("Resource not found");
            assertThat(vm.fieldErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should create ErrorVm with all fields including fieldErrors")
        void errorVm_fullConstructor_shouldCreateCorrectly() {
            List<String> errors = List.of("Field 1 is invalid", "Field 2 is required");
            ErrorVm vm = new ErrorVm("400", "Bad Request", "Validation failed", errors);

            assertThat(vm.statusCode()).isEqualTo("400");
            assertThat(vm.title()).isEqualTo("Bad Request");
            assertThat(vm.detail()).isEqualTo("Validation failed");
            assertThat(vm.fieldErrors()).containsExactlyElementsOf(errors);
        }
    }
}
