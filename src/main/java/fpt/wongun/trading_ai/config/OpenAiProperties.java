package fpt.wongun.trading_ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for OpenAI API.
 */
@ConfigurationProperties(prefix = "openai")
@Data
@Validated
public class OpenAiProperties {

    /**
     * OpenAI API key. Should be set via environment variable OPENAI_API_KEY.
     * REQUIRED if groq.enabled=false
     */
    @NotBlank(message = "OpenAI API key is required when Groq is disabled. Set OPENAI_API_KEY environment variable.")
    private String apiKey;

    /**
     * OpenAI API base URL.
     */
    @NotBlank
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * Model to use for chat completions.
     * Examples: gpt-4, gpt-4-turbo, gpt-3.5-turbo
     */
    @NotBlank
    private String model = "gpt-4o-mini";

    /**
     * Temperature for response generation (0.0 - 2.0).
     * Lower values make output more deterministic.
     */
    @NotNull
    private Double temperature = 0.3;
}
