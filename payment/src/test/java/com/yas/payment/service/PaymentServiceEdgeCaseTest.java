package com.yas.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.Payment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.repository.PaymentRepository;
import com.yas.payment.service.provider.handler.PaymentHandler;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PaymentServiceEdgeCaseTest {

    private PaymentRepository paymentRepository;
    private OrderService orderService;
    private PaymentHandler paypalHandler;
    private PaymentHandler codHandler;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        orderService = mock(OrderService.class);
        paypalHandler = mock(PaymentHandler.class);
        codHandler = mock(PaymentHandler.class);

        when(paypalHandler.getProviderId()).thenReturn(PaymentMethod.PAYPAL.name());
        when(codHandler.getProviderId()).thenReturn(PaymentMethod.COD.name());

        List<PaymentHandler> handlers = new ArrayList<>();
        handlers.add(paypalHandler);
        handlers.add(codHandler);

        paymentService = new PaymentService(paymentRepository, orderService, handlers);
        paymentService.initializeProviders();
    }

    @Nested
    @DisplayName("initPayment tests")
    class InitPaymentTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when provider not found")
        void initPayment_withInvalidProvider_shouldThrowException() {
            InitPaymentRequestVm request = InitPaymentRequestVm.builder()
                    .paymentMethod("UNKNOWN_PROVIDER")
                    .totalPrice(BigDecimal.TEN)
                    .checkoutId("chk-1")
                    .build();

            assertThatThrownBy(() -> paymentService.initPayment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No payment handler found for provider: UNKNOWN_PROVIDER");
        }

        @Test
        @DisplayName("Should successfully init payment with COD provider")
        void initPayment_withCOD_shouldReturnResponse() {
            InitPaymentRequestVm request = InitPaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.COD.name())
                    .totalPrice(BigDecimal.valueOf(50))
                    .checkoutId("chk-cod-1")
                    .build();

            InitiatedPayment initiated = InitiatedPayment.builder()
                    .paymentId("cod-pay-1")
                    .status("CREATED")
                    .redirectUrl(null)
                    .build();

            when(codHandler.initPayment(request)).thenReturn(initiated);

            InitPaymentResponseVm result = paymentService.initPayment(request);

            assertNotNull(result);
            assertEquals("cod-pay-1", result.paymentId());
            assertEquals("CREATED", result.status());
        }

        @Test
        @DisplayName("Should map all fields correctly from InitiatedPayment to response VM")
        void initPayment_shouldMapAllFieldsCorrectly() {
            InitPaymentRequestVm request = InitPaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.PAYPAL.name())
                    .totalPrice(BigDecimal.valueOf(199.99))
                    .checkoutId("chk-map-test")
                    .build();

            InitiatedPayment initiated = InitiatedPayment.builder()
                    .paymentId("pay-map")
                    .status("APPROVED")
                    .redirectUrl("https://paypal.com/redirect")
                    .build();

            when(paypalHandler.initPayment(request)).thenReturn(initiated);

            InitPaymentResponseVm result = paymentService.initPayment(request);

            assertThat(result.paymentId()).isEqualTo("pay-map");
            assertThat(result.status()).isEqualTo("APPROVED");
            assertThat(result.redirectUrl()).isEqualTo("https://paypal.com/redirect");
        }
    }

    @Nested
    @DisplayName("capturePayment tests")
    class CapturePaymentTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when provider not found for capture")
        void capturePayment_withInvalidProvider_shouldThrowException() {
            CapturePaymentRequestVm request = CapturePaymentRequestVm.builder()
                    .paymentMethod("NON_EXISTENT")
                    .token("some-token")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> paymentService.capturePayment(request));

            verify(paymentRepository, never()).save(any());
            verify(orderService, never()).updateCheckoutStatus(any());
        }

        @Test
        @DisplayName("Should handle payment with zero amount")
        void capturePayment_withZeroAmount_shouldSucceed() {
            CapturePaymentRequestVm request = CapturePaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.COD.name())
                    .token("free-token")
                    .build();

            CapturedPayment captured = CapturedPayment.builder()
                    .orderId(null)
                    .checkoutId("chk-free")
                    .amount(BigDecimal.ZERO)
                    .paymentFee(BigDecimal.ZERO)
                    .gatewayTransactionId("gw-free")
                    .paymentMethod(PaymentMethod.COD)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .failureMessage(null)
                    .build();

            Payment savedPayment = new Payment();
            savedPayment.setId(10L);
            savedPayment.setOrderId(55L);
            savedPayment.setPaymentStatus(PaymentStatus.COMPLETED);

            when(codHandler.capturePayment(request)).thenReturn(captured);
            when(orderService.updateCheckoutStatus(captured)).thenReturn(55L);
            when(paymentRepository.save(any())).thenReturn(savedPayment);

            CapturePaymentResponseVm result = paymentService.capturePayment(request);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.paymentFee()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should handle cancelled payment with failure message")
        void capturePayment_withCancelledStatus_shouldPreserveFailureMessage() {
            CapturePaymentRequestVm request = CapturePaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.PAYPAL.name())
                    .token("fail-token")
                    .build();

            CapturedPayment captured = CapturedPayment.builder()
                    .orderId(null)
                    .checkoutId("chk-fail")
                    .amount(BigDecimal.valueOf(100))
                    .paymentFee(BigDecimal.ZERO)
                    .gatewayTransactionId("gw-fail")
                    .paymentMethod(PaymentMethod.PAYPAL)
                    .paymentStatus(PaymentStatus.CANCELLED)
                    .failureMessage("Insufficient funds")
                    .build();

            Payment savedPayment = new Payment();
            savedPayment.setId(20L);
            savedPayment.setOrderId(60L);
            savedPayment.setPaymentStatus(PaymentStatus.CANCELLED);

            when(paypalHandler.capturePayment(request)).thenReturn(captured);
            when(orderService.updateCheckoutStatus(captured)).thenReturn(60L);
            when(paymentRepository.save(any())).thenReturn(savedPayment);

            CapturePaymentResponseVm result = paymentService.capturePayment(request);

            assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.failureMessage()).isEqualTo("Insufficient funds");
        }

        @Test
        @DisplayName("Should set orderId from orderService response on capturedPayment")
        void capturePayment_shouldSetOrderIdFromOrderService() {
            CapturePaymentRequestVm request = CapturePaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.PAYPAL.name())
                    .token("order-token")
                    .build();

            CapturedPayment captured = CapturedPayment.builder()
                    .orderId(null)
                    .checkoutId("chk-order")
                    .amount(BigDecimal.valueOf(200))
                    .paymentFee(BigDecimal.valueOf(5))
                    .gatewayTransactionId("gw-order")
                    .paymentMethod(PaymentMethod.PAYPAL)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .failureMessage(null)
                    .build();

            Long expectedOrderId = 777L;

            Payment savedPayment = new Payment();
            savedPayment.setId(30L);
            savedPayment.setOrderId(expectedOrderId);
            savedPayment.setPaymentStatus(PaymentStatus.COMPLETED);

            when(paypalHandler.capturePayment(request)).thenReturn(captured);
            when(orderService.updateCheckoutStatus(captured)).thenReturn(expectedOrderId);
            when(paymentRepository.save(any())).thenReturn(savedPayment);

            CapturePaymentResponseVm result = paymentService.capturePayment(request);

            // Verify orderId was set on captured payment
            assertThat(captured.getOrderId()).isEqualTo(expectedOrderId);
            assertThat(result.orderId()).isEqualTo(expectedOrderId);
        }
    }

    @Nested
    @DisplayName("initializeProviders tests")
    class InitializeProvidersTests {

        @Test
        @DisplayName("Should register all handlers")
        void initializeProviders_shouldRegisterAllHandlers() {
            // Both PAYPAL and COD handlers were registered in setUp()
            // Verify by successfully initializing payments for both

            InitPaymentRequestVm paypalRequest = InitPaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.PAYPAL.name())
                    .totalPrice(BigDecimal.TEN)
                    .checkoutId("chk-1")
                    .build();

            InitPaymentRequestVm codRequest = InitPaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.COD.name())
                    .totalPrice(BigDecimal.TEN)
                    .checkoutId("chk-2")
                    .build();

            InitiatedPayment initiated = InitiatedPayment.builder()
                    .paymentId("pay-x").status("OK").redirectUrl(null).build();

            when(paypalHandler.initPayment(paypalRequest)).thenReturn(initiated);
            when(codHandler.initPayment(codRequest)).thenReturn(initiated);

            // Should not throw for both providers
            assertNotNull(paymentService.initPayment(paypalRequest));
            assertNotNull(paymentService.initPayment(codRequest));
        }

        @Test
        @DisplayName("Should work with empty handler list")
        void initializeProviders_withNoHandlers_shouldWorkButFailOnGetHandler() {
            PaymentService emptyService = new PaymentService(
                    paymentRepository, orderService, new ArrayList<>());
            emptyService.initializeProviders();

            InitPaymentRequestVm request = InitPaymentRequestVm.builder()
                    .paymentMethod(PaymentMethod.PAYPAL.name())
                    .totalPrice(BigDecimal.TEN)
                    .checkoutId("chk-empty")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> emptyService.initPayment(request));
        }
    }
}
