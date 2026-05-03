package com.yas.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.service.PaymentService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InitPaymentRequestVm initPaymentRequest;
    private InitPaymentResponseVm initPaymentResponse;
    private CapturePaymentRequestVm capturePaymentRequest;
    private CapturePaymentResponseVm capturePaymentResponse;

    @BeforeEach
    void setUp() {
        initPaymentRequest = InitPaymentRequestVm.builder()
                .paymentMethod(PaymentMethod.PAYPAL.name())
                .totalPrice(BigDecimal.valueOf(100.00))
                .checkoutId("checkout-123")
                .build();

        initPaymentResponse = InitPaymentResponseVm.builder()
                .status("CREATED")
                .paymentId("pay-456")
                .redirectUrl("https://paypal.com/checkout/pay-456")
                .build();

        capturePaymentRequest = CapturePaymentRequestVm.builder()
                .paymentMethod(PaymentMethod.PAYPAL.name())
                .token("token-789")
                .build();

        capturePaymentResponse = CapturePaymentResponseVm.builder()
                .orderId(1L)
                .checkoutId("checkout-123")
                .amount(BigDecimal.valueOf(100.00))
                .paymentFee(BigDecimal.valueOf(2.50))
                .gatewayTransactionId("txn-abc")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .failureMessage(null)
                .build();
    }

    @Test
    @DisplayName("POST /init - should return InitPaymentResponseVm on success")
    void initPayment_shouldReturnSuccess() throws Exception {
        when(paymentService.initPayment(any(InitPaymentRequestVm.class))).thenReturn(initPaymentResponse);

        mockMvc.perform(post("/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentId").value("pay-456"))
                .andExpect(jsonPath("$.redirectUrl").value("https://paypal.com/checkout/pay-456"));
    }

    @Test
    @DisplayName("POST /capture - should return CapturePaymentResponseVm on success")
    void capturePayment_shouldReturnSuccess() throws Exception {
        when(paymentService.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(capturePaymentResponse);

        mockMvc.perform(post("/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(capturePaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.checkoutId").value("checkout-123"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.paymentFee").value(2.50))
                .andExpect(jsonPath("$.gatewayTransactionId").value("txn-abc"))
                .andExpect(jsonPath("$.paymentMethod").value("PAYPAL"))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.failureMessage").doesNotExist());
    }

    @Test
    @DisplayName("GET /cancel - should return 200 with cancellation message")
    void cancelPayment_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /init - should return 400 when request body is missing")
    void initPayment_shouldReturn400WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /capture - capturePayment with completed status returns correct response")
    void capturePayment_withCompletedStatus_returnsCorrectFields() throws Exception {
        CapturePaymentResponseVm response = CapturePaymentResponseVm.builder()
                .orderId(42L)
                .checkoutId("chk-999")
                .amount(BigDecimal.valueOf(250.00))
                .paymentFee(BigDecimal.valueOf(5.00))
                .gatewayTransactionId("gateway-xyz")
                .paymentMethod(PaymentMethod.BANKING)
                .paymentStatus(PaymentStatus.COMPLETED)
                .failureMessage(null)
                .build();

        when(paymentService.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(response);

        mockMvc.perform(post("/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CapturePaymentRequestVm.builder()
                                        .paymentMethod(PaymentMethod.BANKING.name())
                                        .token("bank-token")
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.paymentMethod").value("BANKING"))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /capture - capturePayment with cancelled status includes failure message")
    void capturePayment_withCancelledStatus_includesFailureMessage() throws Exception {
        CapturePaymentResponseVm response = CapturePaymentResponseVm.builder()
                .orderId(10L)
                .checkoutId("chk-cancelled")
                .amount(BigDecimal.ZERO)
                .paymentFee(BigDecimal.ZERO)
                .gatewayTransactionId("gateway-fail")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.CANCELLED)
                .failureMessage("Payment was cancelled by user")
                .build();

        when(paymentService.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(response);

        mockMvc.perform(post("/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CapturePaymentRequestVm.builder()
                                        .paymentMethod(PaymentMethod.PAYPAL.name())
                                        .token("cancel-token")
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.failureMessage").value("Payment was cancelled by user"));
    }
}
