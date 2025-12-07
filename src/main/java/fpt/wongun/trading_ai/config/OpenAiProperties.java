package fpt.wongun.trading_ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "openai")
@Data
@Validated
public class OpenAiProperties {

    @NotBlank(message = "OpenAI API key is required when Groq is disabled. Set OPENAI_API_KEY environment variable.")
    private String apiKey;

    @NotBlank
    private String baseUrl = "https://api.openai.com/v1";

    @NotBlank
    private String model = "gpt-4o-mini";

    @NotNull
    private Double temperature = 0.3;
}
