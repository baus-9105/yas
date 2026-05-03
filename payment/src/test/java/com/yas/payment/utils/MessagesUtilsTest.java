package com.yas.payment.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    @DisplayName("getMessage with existing code should return the message")
    void getMessage_withExistingCode_shouldReturnMessage() {
        String result = MessagesUtils.getMessage("SUCCESS_MESSAGE");
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getMessage with existing code and params should format message")
    void getMessage_withExistingCodeAndParams_shouldFormatMessage() {
        String result = MessagesUtils.getMessage("PAYMENT_PROVIDER_NOT_FOUND", "paypal");
        assertThat(result).isEqualTo("Payment provider paypal is not found");
    }

    @Test
    @DisplayName("getMessage with missing code should return the code itself")
    void getMessage_withMissingCode_shouldReturnCode() {
        String result = MessagesUtils.getMessage("MISSING_CODE");
        assertThat(result).isEqualTo("MISSING_CODE");
    }

    @Test
    @DisplayName("getMessage with missing code and params should format code")
    void getMessage_withMissingCodeAndParams_shouldFormatCode() {
        String result = MessagesUtils.getMessage("MISSING_CODE_{}", "param1");
        assertThat(result).isEqualTo("MISSING_CODE_param1");
    }
}
