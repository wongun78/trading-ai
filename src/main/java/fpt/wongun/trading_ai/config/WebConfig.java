package fpt.wongun.trading_ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS (Cross-Origin Resource Sharing).
 * Restricts API access to specific frontend origins for security.
 * 
 * PRODUCTION NOTE: Configure ALLOWED_ORIGINS environment variable with your production domain.
 * Example: ALLOWED_ORIGINS=https://myapp.com,https://www.myapp.com
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                // Only allow necessary HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // allowCredentials requires specific origins (not *)
                .allowCredentials(true)
                .maxAge(maxAge);  // Cache preflight requests
    }
}
