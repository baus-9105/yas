package com.yas.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    @Test
    @DisplayName("getRestClient should build RestClient")
    void getRestClient_shouldBuild() {
        RestClientConfig config = new RestClientConfig();
        RestClient.Builder builder = RestClient.builder();
        RestClient restClient = config.getRestClient(builder);
        assertThat(restClient).isNotNull();
    }
}
