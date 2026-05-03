package com.yas.order.config;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("jwtAuthenticationConverterForKeycloak should convert roles")
    void jwtAuthenticationConverterForKeycloak_shouldConvertRoles() {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverterForKeycloak();
        
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
            .build();
            
        var auth = converter.convert(jwt);
        assertThat(auth).isNotNull();
        
        Collection<GrantedAuthority> authorities = auth.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_ADMIN", "ROLE_USER");
    }
}
