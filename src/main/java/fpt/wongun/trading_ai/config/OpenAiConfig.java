package fpt.wongun.trading_ai.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, GroqProperties.class})
public class OpenAiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "groq", name = "enabled", havingValue = "false", matchIfMissing = false)
    public WebClient openAiWebClient(OpenAiProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))  // AI responses can take time
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000); // 10s connect timeout

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
