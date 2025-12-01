package fpt.wongun.trading_ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Groq API.
 * Groq provides fast, free inference for open-source LLMs like Llama 3.
 */
@ConfigurationProperties(prefix = "groq")
@Data
@Validated
public class GroqProperties {

    /**
     * Groq API key. Get free API key at https://console.groq.com
     * MUST be set via environment variable GROQ_API_KEY
     */
    @NotBlank(message = "Groq API key is required. Set GROQ_API_KEY environment variable.")
    private String apiKey;

    /**
     * Groq API base URL.
     */
    @NotBlank
    private String baseUrl = "https://api.groq.com/openai/v1";

    /**
     * Model to use for chat completions.
     * Examples: llama-3.3-70b-versatile, llama-3.1-70b-versatile, mixtral-8x7b-32768
     */
    @NotBlank
    private String model = "llama-3.3-70b-versatile";

    /**
     * Temperature for response generation (0.0 - 2.0).
     * Lower values make output more deterministic.
     */
    @NotNull
    private Double temperature = 0.3;

    /**
     * Enable or disable Groq client. If false, falls back to OpenAI or Mock.
     */
    private boolean enabled = true;
}
