package com.yas.payment.service.provider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaypalHandlerTest {

    private PaymentProviderService paymentProviderService;
    private PaypalService paypalService;
    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        paymentProviderService = mock(PaymentProviderService.class);
        paypalService = mock(PaypalService.class);
        paypalHandler = new PaypalHandler(paymentProviderService, paypalService);
    }

    @Test
    @DisplayName("getProviderId should return PAYPAL")
    void getProviderId_shouldReturnPaypal() {
        assertThat(paypalHandler.getProviderId()).isEqualTo(PaymentMethod.PAYPAL.name());
    }

    @Test
    @DisplayName("initPayment should call paypalService and return InitiatedPayment")
    void initPayment_shouldReturnInitiatedPayment() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .totalPrice(BigDecimal.valueOf(100))
                .checkoutId("chk-123")
                .build();

        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId("PAYPAL"))
                .thenReturn("{\"clientId\": \"abc\"}");

        PaypalCreatePaymentResponse paypalResponse = PaypalCreatePaymentResponse.builder()
                .status("CREATED")
                .paymentId("pay-456")
                .redirectUrl("http://paypal.com/pay-456")
                .build();

        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class)))
                .thenReturn(paypalResponse);

        InitiatedPayment result = paypalHandler.initPayment(requestVm);

        ArgumentCaptor<PaypalCreatePaymentRequest> captor = ArgumentCaptor.forClass(PaypalCreatePaymentRequest.class);
        verify(paypalService).createPayment(captor.capture());

        PaypalCreatePaymentRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(capturedRequest.checkoutId()).isEqualTo("chk-123");
        assertThat(capturedRequest.paymentMethod()).isEqualTo("PAYPAL");
        assertThat(capturedRequest.paymentSettings()).isEqualTo("{\"clientId\": \"abc\"}");

        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getPaymentId()).isEqualTo("pay-456");
        assertThat(result.getRedirectUrl()).isEqualTo("http://paypal.com/pay-456");
    }

    @Test
    @DisplayName("capturePayment should call paypalService and return CapturedPayment")
    void capturePayment_shouldReturnCapturedPayment() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .token("token-789")
                .build();

        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId("PAYPAL"))
                .thenReturn("{\"clientId\": \"abc\"}");

        PaypalCapturePaymentResponse paypalResponse = PaypalCapturePaymentResponse.builder()
                .checkoutId("chk-123")
                .amount(BigDecimal.valueOf(100))
                .paymentFee(BigDecimal.valueOf(5))
                .gatewayTransactionId("txn-xyz")
                .paymentMethod("PAYPAL")
                .paymentStatus("COMPLETED")
                .failureMessage(null)
                .build();

        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class)))
                .thenReturn(paypalResponse);

        CapturedPayment result = paypalHandler.capturePayment(requestVm);

        ArgumentCaptor<PaypalCapturePaymentRequest> captor = ArgumentCaptor.forClass(PaypalCapturePaymentRequest.class);
        verify(paypalService).capturePayment(captor.capture());

        PaypalCapturePaymentRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.token()).isEqualTo("token-789");
        assertThat(capturedRequest.paymentSettings()).isEqualTo("{\"clientId\": \"abc\"}");

        assertThat(result.getCheckoutId()).isEqualTo("chk-123");
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getPaymentFee()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(result.getGatewayTransactionId()).isEqualTo("txn-xyz");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getFailureMessage()).isNull();
    }
}
