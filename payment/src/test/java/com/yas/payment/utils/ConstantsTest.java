package com.yas.payment.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    @DisplayName("ErrorCode PAYMENT_PROVIDER_NOT_FOUND should have correct value")
    void errorCode_paymentProviderNotFound_shouldHaveCorrectValue() {
        assertThat(Constants.ErrorCode.PAYMENT_PROVIDER_NOT_FOUND).isEqualTo("PAYMENT_PROVIDER_NOT_FOUND");
    }

    @Test
    @DisplayName("Message SUCCESS_MESSAGE should have correct value")
    void message_successMessage_shouldHaveCorrectValue() {
        assertThat(Constants.Message.SUCCESS_MESSAGE).isEqualTo("SUCCESS");
    }
}
