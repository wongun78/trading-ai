package fpt.wongun.trading_ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for AI API clients (OpenAI and Groq).
 * 
 * This configuration supports multiple AI providers:
 * - Groq (free, fast, open-source models) - enabled by default
 * - OpenAI (paid, GPT models) - fallback when Groq is disabled
 */
@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, GroqProperties.class})
public class OpenAiConfig {

    /**
     * WebClient bean for making requests to OpenAI API.
     * Only created when groq.enabled=false (fallback to OpenAI).
     */
    @Bean
    @ConditionalOnProperty(prefix = "groq", name = "enabled", havingValue = "false", matchIfMissing = false)
    public WebClient openAiWebClient(OpenAiProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
