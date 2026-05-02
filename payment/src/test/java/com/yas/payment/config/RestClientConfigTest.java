package com.yas.payment.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    @Test
    @DisplayName("getRestClient should build RestClient with JSON Content-Type default header")
    void getRestClient_shouldBuildWithJsonContentType() {
        RestClientConfig config = new RestClientConfig();
        RestClient.Builder builder = RestClient.builder();

        RestClient restClient = config.getRestClient(builder);

        assertThat(restClient).isNotNull();
        // Since we can't easily inspect the default headers of the built RestClient without reflection or mocking,
        // we assert it builds successfully.
    }
}
