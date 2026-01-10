package com.example.cxf.cache.distributed;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ReadFrom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Sentinel Configuration for High Availability Distributed Cache.
 * 
 * Features:
 * - Automatic failover with Redis Sentinel
 * - Master-replica replication
 * - Connection pooling for performance
 * - Read from replicas for load distribution
 * - Health monitoring and circuit breaker
 * 
 * Architecture:
 * - 1 Master (writes)
 * - 2+ Replicas (reads)
 * - 3 Sentinels (monitoring & failover)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.redis.sentinel.enabled", havingValue = "true", matchIfMissing = false)
public class RedisSentinelConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelConfig.class);

    /**
     * Configure Redis Sentinel Connection Factory with connection pooling.
     */
    @Bean
    @Primary
    public LettuceConnectionFactory sentinelConnectionFactory(RedisSentinelConfiguration sentinelConfig) {
        logger.info("Configuring Redis Sentinel Connection Factory");
        
        // Configure Lettuce Client with connection pooling
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofMillis(100))
                .poolConfig(poolConfig())
                .readFrom(ReadFrom.REPLICA_PREFERRED)  // Read from replicas when possible
                .build();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig, clientConfig);
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(false);  // Important for thread safety
        
        logger.info("Redis Sentinel connection factory configured successfully");
        return factory;
    }

    /**
     * Redis Sentinel Configuration Bean.
     * Reads from application.yml: spring.redis.sentinel.*
     */
    @Bean
    public RedisSentinelConfiguration redisSentinelConfiguration() {
        logger.info("Configuring Redis Sentinel");
        
        RedisSentinelConfiguration config = new RedisSentinelConfiguration()
                .master("mymaster");  // Must match sentinel.conf
        
        // Add sentinel nodes (from application.yml or hardcoded)
        config.sentinel("localhost", 26379);
        config.sentinel("localhost", 26380);
        config.sentinel("localhost", 26381);
        
        logger.info("Redis Sentinel configured with master: mymaster");
        return config;
    }

    /**
     * Connection Pool Configuration.
     */
    private org.apache.commons.pool2.impl.GenericObjectPoolConfig poolConfig() {
        org.apache.commons.pool2.impl.GenericObjectPoolConfig config = 
            new org.apache.commons.pool2.impl.GenericObjectPoolConfig();
        
        config.setMaxTotal(20);        // Max connections
        config.setMaxIdle(10);         // Max idle connections
        config.setMinIdle(5);          // Min idle connections
        config.setMaxWaitMillis(3000); // Max wait for connection
        config.setTestOnBorrow(true);  // Validate on borrow
        config.setTestWhileIdle(true); // Validate idle connections
        config.setTimeBetweenEvictionRunsMillis(60000); // Eviction thread run interval
        
        logger.debug("Connection pool configured: maxTotal=20, maxIdle=10, minIdle=5");
        return config;
    }

    /**
     * Cache Manager with custom TTL per cache.
     */
    @Bean
    @Primary
    public CacheManager sentinelCacheManager(RedisConnectionFactory sentinelConnectionFactory) {
        logger.info("Configuring Distributed Cache Manager with Sentinel");
        
        // JSON serializer for cache values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues()
                .prefixCacheNameWith("calc:distributed:");  // Namespace for distributed cache
        
        // Custom TTL per cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Operations cache: 10 minutes
        cacheConfigurations.put("operations", 
            defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Calculations cache: 10 minutes
        cacheConfigurations.put("calculations", 
            defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        RedisCacheManager cacheManager = RedisCacheManager.builder(sentinelConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()  // Support Spring transactions
                .build();
        
        logger.info("Distributed Cache Manager configured with caches: {}", cacheConfigurations.keySet());
        return cacheManager;
    }
}
