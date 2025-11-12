package com.dtao.lms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * ✅ CacheConfig
 *
 * Enables high-performance in-memory caching using Caffeine.
 * This dramatically reduces MongoDB load during high concurrency (10k+ leaderboard reads).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        // Configure cache behavior: TTL + max size
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)  // cache items live for 5 minutes
                .maximumSize(5000)                      // max 5000 leaderboard entries
                .recordStats();                         // enable hit/miss metrics
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "globalLeaderboard", "examLeaderboards"
        );
        cacheManager.setCaffeine(caffeine);
        log.info("✅ Caffeine cache initialized (TTL=5min, maxSize=5000)");
        return cacheManager;
    }
}
