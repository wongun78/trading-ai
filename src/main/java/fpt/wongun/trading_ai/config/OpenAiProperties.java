package fpt.wongun.trading_ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenAI API.
 */
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAiProperties {

    /**
     * OpenAI API key. Should be set via environment variable OPENAI_API_KEY.
     */
    private String apiKey = "changeme";

    /**
     * OpenAI API base URL.
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * Model to use for chat completions.
     * Examples: gpt-4, gpt-4-turbo, gpt-3.5-turbo
     */
    private String model = "gpt-4o-mini";

    /**
     * Temperature for response generation (0.0 - 2.0).
     * Lower values make output more deterministic.
     */
    private Double temperature = 0.3;
}
