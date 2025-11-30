package fpt.wongun.trading_ai.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for AI signal generation.
 * Caches AI responses for 30 seconds to prevent rate limiting from Groq API.
 * 
 * Free tier limits: 30 requests/minute = 1 request per 2 seconds
 * Cache duration: 30 seconds to balance freshness and API quota
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Cache with 30-second TTL
        ConcurrentMapCache aiSignalsCache = new ConcurrentMapCache("aiSignals", 
            new ConcurrentHashMap<>(), 
            false) {
            
            private final long TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);
            private final ConcurrentHashMap<Object, Long> expirationMap = new ConcurrentHashMap<>();
            
            @Override
            public ValueWrapper get(Object key) {
                Long expirationTime = expirationMap.get(key);
                if (expirationTime != null && System.currentTimeMillis() > expirationTime) {
                    // Expired - remove from cache
                    evict(key);
                    expirationMap.remove(key);
                    return null;
                }
                return super.get(key);
            }
            
            @Override
            public void put(Object key, Object value) {
                super.put(key, value);
                expirationMap.put(key, System.currentTimeMillis() + TTL_MILLIS);
            }
            
            @Override
            public void evict(Object key) {
                super.evict(key);
                expirationMap.remove(key);
            }
        };
        
        cacheManager.setCaches(Collections.singletonList(aiSignalsCache));
        return cacheManager;
    }
}
