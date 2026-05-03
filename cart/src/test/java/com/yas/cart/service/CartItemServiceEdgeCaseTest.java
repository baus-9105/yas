package com.yas.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.cart.mapper.CartItemMapper;
import com.yas.cart.model.CartItem;
import com.yas.cart.repository.CartItemRepository;
import com.yas.cart.viewmodel.CartItemDeleteVm;
import com.yas.cart.viewmodel.CartItemGetVm;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CartItemServiceEdgeCaseTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    @Spy
    private CartItemMapper cartItemMapper = new CartItemMapper();

    @InjectMocks
    private CartItemService cartItemService;

    private static final String USER_ID = "test-user";

    private void mockCurrentUserId(String userId) {
        Jwt jwt = mock(Jwt.class);
        JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(jwtToken);
        when(jwt.getSubject()).thenReturn(userId);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("deleteCartItem tests")
    class DeleteCartItemTests {

        @Test
        @DisplayName("Should call repository deleteByCustomerIdAndProductId")
        void deleteCartItem_shouldCallRepository() {
            mockCurrentUserId(USER_ID);

            cartItemService.deleteCartItem(10L);

            verify(cartItemRepository).deleteByCustomerIdAndProductId(USER_ID, 10L);
        }

        @Test
        @DisplayName("Should use current user ID from security context")
        void deleteCartItem_shouldUseCurrentUserId() {
            mockCurrentUserId("another-user");

            cartItemService.deleteCartItem(20L);

            verify(cartItemRepository).deleteByCustomerIdAndProductId("another-user", 20L);
        }
    }

    @Nested
    @DisplayName("getCartItems edge cases")
    class GetCartItemsEdgeCaseTests {

        @Test
        @DisplayName("Should return empty list when user has no cart items")
        void getCartItems_whenNoItems_shouldReturnEmptyList() {
            mockCurrentUserId(USER_ID);
            when(cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(USER_ID))
                    .thenReturn(Collections.emptyList());

            List<CartItemGetVm> result = cartItemService.getCartItems();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return multiple items ordered by createdOn")
        void getCartItems_shouldReturnMultipleItems() {
            mockCurrentUserId(USER_ID);
            CartItem item1 = CartItem.builder().customerId(USER_ID).productId(1L).quantity(2).build();
            CartItem item2 = CartItem.builder().customerId(USER_ID).productId(2L).quantity(5).build();

            when(cartItemRepository.findByCustomerIdOrderByCreatedOnDesc(USER_ID))
                    .thenReturn(List.of(item1, item2));

            List<CartItemGetVm> result = cartItemService.getCartItems();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).productId()).isEqualTo(1L);
            assertThat(result.get(0).quantity()).isEqualTo(2);
            assertThat(result.get(1).productId()).isEqualTo(2L);
            assertThat(result.get(1).quantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("deleteOrAdjustCartItem edge cases")
    class DeleteOrAdjustEdgeCaseTests {

        @Test
        @DisplayName("Should handle when cart item not found in DB (product not in cart)")
        void deleteOrAdjust_whenCartItemNotInDb_shouldSkip() {
            mockCurrentUserId(USER_ID);
            CartItemDeleteVm deleteVm = new CartItemDeleteVm(999L, 1);

            when(cartItemRepository.findByCustomerIdAndProductIdIn(anyString(), any()))
                    .thenReturn(Collections.emptyList());
            when(cartItemRepository.saveAll(any())).thenReturn(Collections.emptyList());

            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of(deleteVm));

            assertThat(result).isEmpty();
            verify(cartItemRepository).deleteAll(Collections.emptyList());
        }

        @Test
        @DisplayName("Should delete when quantity equals cart item quantity exactly")
        void deleteOrAdjust_whenQuantityEquals_shouldDelete() {
            mockCurrentUserId(USER_ID);
            CartItem existingItem = CartItem.builder()
                    .customerId(USER_ID)
                    .productId(1L)
                    .quantity(3)
                    .build();
            CartItemDeleteVm deleteVm = new CartItemDeleteVm(1L, 3);

            when(cartItemRepository.findByCustomerIdAndProductIdIn(anyString(), any()))
                    .thenReturn(List.of(existingItem));
            when(cartItemRepository.saveAll(any())).thenReturn(Collections.emptyList());

            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of(deleteVm));

            verify(cartItemRepository).deleteAll(List.of(existingItem));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty delete list")
        void deleteOrAdjust_emptyList_shouldReturnEmpty() {
            when(cartItemRepository.saveAll(any())).thenReturn(Collections.emptyList());

            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should allow same product with same quantity (not duplicated)")
        void deleteOrAdjust_sameProductSameQuantity_shouldNotThrow() {
            mockCurrentUserId(USER_ID);
            CartItemDeleteVm deleteVm1 = new CartItemDeleteVm(1L, 2);
            CartItemDeleteVm deleteVm2 = new CartItemDeleteVm(1L, 2);

            CartItem existingItem = CartItem.builder()
                    .customerId(USER_ID)
                    .productId(1L)
                    .quantity(5)
                    .build();

            when(cartItemRepository.findByCustomerIdAndProductIdIn(anyString(), any()))
                    .thenReturn(List.of(existingItem));
            when(cartItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            // Same product with same quantity should NOT throw (only different quantities throw)
            List<CartItemGetVm> result = cartItemService.deleteOrAdjustCartItem(List.of(deleteVm1, deleteVm2));

            assertThat(result).isNotNull();
        }
    }
}
