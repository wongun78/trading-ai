package fpt.wongun.trading_ai.config;

import fpt.wongun.trading_ai.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JpaAuditConfig {

    private final SecurityUtils securityUtils;

    /**
     * Provides the current auditor (user) for audit fields.
     * Returns authenticated username or "SYSTEM" if no authentication context.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(securityUtils.getCurrentUsername());
    }
}
