package com.yas.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentProviderController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentProviderService paymentProviderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /backoffice/payment-providers - should create and return 201")
    void create_shouldReturn201WithCreatedProvider() throws Exception {
        CreatePaymentVm createPaymentVm = new CreatePaymentVm();
        createPaymentVm.setId("paypal");
        createPaymentVm.setName("PayPal");
        createPaymentVm.setConfigureUrl("https://paypal.com/config");
        createPaymentVm.setEnabled(true);

        PaymentProviderVm response = new PaymentProviderVm(
                "paypal", "PayPal", "https://paypal.com/config", 1, null, null
        );

        when(paymentProviderService.create(any(CreatePaymentVm.class))).thenReturn(response);

        mockMvc.perform(post("/backoffice/payment-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentVm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("paypal"))
                .andExpect(jsonPath("$.name").value("PayPal"))
                .andExpect(jsonPath("$.configureUrl").value("https://paypal.com/config"));
    }

    @Test
    @DisplayName("PUT /backoffice/payment-providers - should update and return 200")
    void update_shouldReturn200WithUpdatedProvider() throws Exception {
        UpdatePaymentVm updatePaymentVm = new UpdatePaymentVm();
        updatePaymentVm.setId("paypal");
        updatePaymentVm.setName("PayPal Updated");
        updatePaymentVm.setConfigureUrl("https://paypal.com/config-v2");
        updatePaymentVm.setEnabled(true);

        PaymentProviderVm response = new PaymentProviderVm(
                "paypal", "PayPal Updated", "https://paypal.com/config-v2", 2, null, null
        );

        when(paymentProviderService.update(any(UpdatePaymentVm.class))).thenReturn(response);

        mockMvc.perform(put("/backoffice/payment-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePaymentVm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("paypal"))
                .andExpect(jsonPath("$.name").value("PayPal Updated"))
                .andExpect(jsonPath("$.configureUrl").value("https://paypal.com/config-v2"));
    }

    @Test
    @DisplayName("GET /storefront/payment-providers - should return list of enabled providers")
    void getAll_shouldReturnListOfEnabledProviders() throws Exception {
        PaymentProviderVm provider1 = new PaymentProviderVm(
                "paypal", "PayPal", "https://paypal.com/config", 1, 100L, "https://media.com/paypal.png"
        );
        PaymentProviderVm provider2 = new PaymentProviderVm(
                "banking", "Banking", "https://bank.com/config", 1, 101L, "https://media.com/bank.png"
        );

        when(paymentProviderService.getEnabledPaymentProviders(any(Pageable.class)))
                .thenReturn(List.of(provider1, provider2));

        mockMvc.perform(get("/storefront/payment-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("paypal"))
                .andExpect(jsonPath("$[0].name").value("PayPal"))
                .andExpect(jsonPath("$[0].iconUrl").value("https://media.com/paypal.png"))
                .andExpect(jsonPath("$[1].id").value("banking"))
                .andExpect(jsonPath("$[1].name").value("Banking"));
    }

    @Test
    @DisplayName("GET /storefront/payment-providers - should return empty list when no enabled providers")
    void getAll_shouldReturnEmptyListWhenNoProviders() throws Exception {
        when(paymentProviderService.getEnabledPaymentProviders(any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/storefront/payment-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /storefront/payment-providers - should accept pagination parameters")
    void getAll_shouldAcceptPaginationParams() throws Exception {
        when(paymentProviderService.getEnabledPaymentProviders(any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/storefront/payment-providers")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /backoffice/payment-providers - create provider with media id")
    void create_withMediaId_shouldReturnProviderWithMedia() throws Exception {
        CreatePaymentVm createPaymentVm = new CreatePaymentVm();
        createPaymentVm.setId("cod");
        createPaymentVm.setName("Cash on Delivery");
        createPaymentVm.setConfigureUrl("https://cod.com/config");
        createPaymentVm.setEnabled(true);
        createPaymentVm.setMediaId(200L);

        PaymentProviderVm response = new PaymentProviderVm(
                "cod", "Cash on Delivery", "https://cod.com/config", 1, 200L, null
        );

        when(paymentProviderService.create(any(CreatePaymentVm.class))).thenReturn(response);

        mockMvc.perform(post("/backoffice/payment-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentVm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("cod"))
                .andExpect(jsonPath("$.name").value("Cash on Delivery"))
                .andExpect(jsonPath("$.mediaId").value(200));
    }
}
