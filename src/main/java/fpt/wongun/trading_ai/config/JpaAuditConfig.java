package fpt.wongun.trading_ai.config;

import fpt.wongun.trading_ai.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditConfig {

    private final SecurityUtils securityUtils;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(securityUtils.getCurrentUsername());
    }
}
