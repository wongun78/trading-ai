package fpt.wongun.trading_ai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for AI signal generation using Caffeine.
 * Caffeine is a high-performance, near-optimal caching library recommended by Spring.
 * 
 * Features:
 * - W-TinyLFU eviction policy (better than LRU)
 * - Automatic TTL with expireAfterWrite
 * - High concurrency performance
 * - Built-in metrics support
 * 
 * Cache duration: 30 seconds to balance freshness and API quota.
 * Free tier limits: 30 requests/minute = 1 request per 2 seconds
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("aiSignals");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(100) // Max 100 cached signals
                .recordStats()); // Enable metrics for monitoring
        return cacheManager;
    }
}
