package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.yas.cart.viewmodel.ProductThumbnailVm;
import com.yas.commonlibrary.config.ServiceUrlConfig;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class ProductServiceEdgeCaseTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private ProductService productService;
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        restClient = Mockito.mock(RestClient.class);
        serviceUrlConfig = Mockito.mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
    }

    private void mockGetProducts(List<Long> ids, List<ProductThumbnailVm> response) {
        URI url = UriComponentsBuilder
                .fromUriString("http://api.yas.local/product")
                .path("/storefront/products/list-featured")
                .queryParam("productId", ids)
                .build()
                .toUri();

        when(serviceUrlConfig.product()).thenReturn("http://api.yas.local/product");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(new ParameterizedTypeReference<List<ProductThumbnailVm>>() {}))
                .thenReturn(ResponseEntity.ok(response));
    }

    @Nested
    @DisplayName("getProductById tests")
    class GetProductByIdTests {

        @Test
        @DisplayName("Should return product when product exists")
        void getProductById_whenProductExists_shouldReturnProduct() {
            ProductThumbnailVm product = new ProductThumbnailVm(1L, "Phone", "phone", "http://img.com/phone.jpg");
            mockGetProducts(List.of(1L), List.of(product));

            ProductThumbnailVm result = productService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Phone");
            assertThat(result.slug()).isEqualTo("phone");
        }

        @Test
        @DisplayName("Should return null when product list is empty")
        void getProductById_whenProductNotFound_shouldReturnNull() {
            mockGetProducts(List.of(999L), Collections.emptyList());

            ProductThumbnailVm result = productService.getProductById(999L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when product list is null")
        void getProductById_whenResponseIsNull_shouldReturnNull() {
            URI url = UriComponentsBuilder
                    .fromUriString("http://api.yas.local/product")
                    .path("/storefront/products/list-featured")
                    .queryParam("productId", List.of(500L))
                    .build()
                    .toUri();

            when(serviceUrlConfig.product()).thenReturn("http://api.yas.local/product");
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toEntity(new ParameterizedTypeReference<List<ProductThumbnailVm>>() {}))
                    .thenReturn(ResponseEntity.ok(null));

            ProductThumbnailVm result = productService.getProductById(500L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("existsById tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when product exists")
        void existsById_whenProductExists_shouldReturnTrue() {
            ProductThumbnailVm product = new ProductThumbnailVm(1L, "Laptop", "laptop", "http://img.com/laptop.jpg");
            mockGetProducts(List.of(1L), List.of(product));

            boolean result = productService.existsById(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when product does not exist")
        void existsById_whenProductNotFound_shouldReturnFalse() {
            mockGetProducts(List.of(999L), Collections.emptyList());

            boolean result = productService.existsById(999L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handleProductThumbnailFallback tests")
    class FallbackTests {

        @Test
        @DisplayName("Should rethrow the original exception")
        void handleProductThumbnailFallback_shouldRethrowException() {
            RuntimeException originalException = new RuntimeException("Connection timeout");

            assertThatThrownBy(() -> productService.handleProductThumbnailFallback(originalException))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection timeout");
        }
    }
}
