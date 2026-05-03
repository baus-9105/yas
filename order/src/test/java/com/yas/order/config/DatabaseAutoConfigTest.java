package com.yas.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DatabaseAutoConfigTest {

    @Test
    @DisplayName("auditorAware should return empty when no authentication")
    void auditorAware_shouldReturnEmpty() {
        DatabaseAutoConfig config = new DatabaseAutoConfig();
        AuditorAware<String> auditorAware = config.auditorAware();
        
        SecurityContextHolder.clearContext();
        
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).isPresent().contains("");
    }

    @Test
    @DisplayName("auditorAware should return username when authenticated")
    void auditorAware_shouldReturnUsername() {
        DatabaseAutoConfig config = new DatabaseAutoConfig();
        AuditorAware<String> auditorAware = config.auditorAware();
        
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("testuser", "password")
        );
        
        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).isPresent().contains("testuser");
        
        SecurityContextHolder.clearContext();
    }
}
