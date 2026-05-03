package com.yas.payment.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class DatabaseAutoConfigTest {

    private DatabaseAutoConfig databaseAutoConfig;

    @BeforeEach
    void setUp() {
        databaseAutoConfig = new DatabaseAutoConfig();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("auditorAware should return empty string when Authentication is null")
    void auditorAware_shouldReturnEmptyString_whenAuthenticationIsNull() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        AuditorAware<String> auditorAware = databaseAutoConfig.auditorAware();
        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertThat(currentAuditor).isPresent().contains("");
    }

    @Test
    @DisplayName("auditorAware should return username when Authentication is not null")
    void auditorAware_shouldReturnUsername_whenAuthenticationIsNotNull() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test-user");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        AuditorAware<String> auditorAware = databaseAutoConfig.auditorAware();
        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertThat(currentAuditor).isPresent().contains("test-user");
    }
}
