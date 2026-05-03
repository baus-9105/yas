package com.yas.payment.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class SecurityConfigTest {

    @Test
    @DisplayName("jwtAuthenticationConverterForKeycloak should convert roles from realm_access to authorities")
    void jwtAuthenticationConverterForKeycloak_shouldConvertRolesToAuthorities() {
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverterForKeycloak();

        Jwt jwt = mock(Jwt.class);
        Map<String, Collection<String>> realmAccess = Map.of("roles", List.of("ADMIN", "USER"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        var authentication = converter.convert(jwt);
        
        assertThat(authentication).isNotNull();
        Collection<GrantedAuthority> authorities = authentication.getAuthorities();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_ADMIN", "ROLE_USER");
    }
}
