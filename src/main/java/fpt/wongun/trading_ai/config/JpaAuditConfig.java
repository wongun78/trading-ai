package fpt.wongun.trading_ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing configuration.
 * Automatically populates createdBy, lastModifiedBy, createdAt, updatedAt fields.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {

    /**
     * Provides the current auditor (user) for audit fields.
     * TODO: Replace with actual user from SecurityContext when authentication is implemented.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // TODO: Get from SecurityContext
            // return Optional.ofNullable(SecurityContextHolder.getContext())
            //     .map(SecurityContext::getAuthentication)
            //     .filter(Authentication::isAuthenticated)
            //     .map(Authentication::getName);
            
            return Optional.of("SYSTEM");
        };
    }
}
