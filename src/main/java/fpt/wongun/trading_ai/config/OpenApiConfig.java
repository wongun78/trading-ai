package fpt.wongun.trading_ai.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Trading AI API",
        version = "2.0.0",
        description = "Enterprise-grade AI Trading System based on Bob Volman Price Action methodology. " +
                     "Uses Groq AI (Llama 3.3 70B) + OpenAI GPT-4 fallback for intelligent trade signal generation.",
        contact = @Contact(
            name = "Trading AI Team",
            email = "support@trading-ai.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            description = "Local Development",
            url = "http://localhost:8080"
        ),
        @Server(
            description = "Production",
            url = "https://api.trading-ai.com"
        )
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT Bearer token authentication. Obtain token from /api/auth/login endpoint.",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
