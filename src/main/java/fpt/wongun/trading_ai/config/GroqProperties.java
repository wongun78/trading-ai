package fpt.wongun.trading_ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "groq")
@Data
@Validated
public class GroqProperties {

    @NotBlank(message = "Groq API key is required. Set GROQ_API_KEY environment variable.")
    private String apiKey;

    @NotBlank
    private String baseUrl = "https://api.groq.com/openai/v1";

    @NotBlank
    private String model = "llama-3.3-70b-versatile";

    @NotNull
    private Double temperature = 0.3;

    private boolean enabled = true;
}
