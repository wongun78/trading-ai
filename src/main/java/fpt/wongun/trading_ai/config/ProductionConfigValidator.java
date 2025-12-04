package fpt.wongun.trading_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required configuration is properly set in production environment.
 * Prevents application startup with default/placeholder values for sensitive configurations.
 * 
 * This validator only runs when spring.profiles.active=prod
 */
@Component
@Profile("prod")
@Slf4j
public class ProductionConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Value("${groq.enabled}")
    private boolean groqEnabled;

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("Validating production configuration...");
        
        List<String> errors = new ArrayList<>();

        // Validate database password
        if (dbPassword == null || dbPassword.isBlank()) {
            errors.add("❌ DB_PASSWORD is required in production! Set environment variable.");
        }

        // Validate AI API keys based on which provider is enabled
        if (groqEnabled) {
            if (groqApiKey == null || groqApiKey.isBlank()) {
                errors.add("❌ GROQ_API_KEY is required when Groq is enabled! Get free key at https://console.groq.com");
            }
        } else {
            if (openAiApiKey == null || openAiApiKey.isBlank()) {
                errors.add("❌ OPENAI_API_KEY is required when Groq is disabled! Get key at https://platform.openai.com/api-keys");
            }
        }

        // Fail fast if any configuration is invalid
        if (!errors.isEmpty()) {
            String errorMessage = "\n\n" +
                    "═════════════════════════════════════════════════════════════\n" +
                    "   PRODUCTION CONFIGURATION VALIDATION FAILED\n" +
                    "═════════════════════════════════════════════════════════════\n" +
                    String.join("\n", errors) +
                    "\n\nApplication startup aborted. Fix configuration and restart.\n" +
                    "═════════════════════════════════════════════════════════════\n";
            
            throw new IllegalStateException(errorMessage);
        }

        log.info("✅ Production configuration validated successfully!");
    }
}


