package com.example.cxf.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration for Calculator Operations.
 * 
 * Features:
 * - JSON serialization for cache values
 * - Custom TTL per cache name
 * - Key prefix for namespace isolation
 * - Error handling to prevent cache failures from affecting operations
 */
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);

    /**
     * Configure Redis Cache Manager with custom settings per cache.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        logger.info("Configuring Redis Cache Manager");
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // Default TTL: 5 minutes
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper())
                        )
                )
                .disableCachingNullValues()
                .prefixCacheNameWith("calculator:");

        // Custom configurations for specific caches
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Cache for calculation results - 5 minutes TTL
        cacheConfigurations.put("calculations", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Cache for operation results - 10 minutes TTL (simple operations)
        cacheConfigurations.put("operations", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        logger.info("Redis Cache Manager configured successfully with caches: {}", 
                    cacheConfigurations.keySet());
        
        return cacheManager;
    }

    /**
     * ObjectMapper for JSON serialization with type information.
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    /**
     * Custom error handler to prevent cache failures from affecting application.
     * Logs errors but allows operations to proceed.
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, 
                                           org.springframework.cache.Cache cache, Object key) {
                logger.error("Cache GET error for cache '{}' with key '{}': {}", 
                           cache.getName(), key, exception.getMessage());
                // Allow operation to proceed without cached value
            }

            @Override
            public void handleCachePutError(RuntimeException exception, 
                                           org.springframework.cache.Cache cache, Object key, Object value) {
                logger.error("Cache PUT error for cache '{}' with key '{}': {}", 
                           cache.getName(), key, exception.getMessage());
                // Allow operation to complete without caching
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, 
                                             org.springframework.cache.Cache cache, Object key) {
                logger.error("Cache EVICT error for cache '{}' with key '{}': {}", 
                           cache.getName(), key, exception.getMessage());
                // Allow operation to proceed
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, 
                                             org.springframework.cache.Cache cache) {
                logger.error("Cache CLEAR error for cache '{}': {}", 
                           cache.getName(), exception.getMessage());
                // Allow operation to proceed
            }
        };
    }
}
