package com.example.cxf.cache.distributed;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Distributed Cache Metrics Collector.
 * 
 * Collects metrics for:
 * - Cache hit rate
 * - Cache miss rate
 * - Cache operation latency
 * - Cache size and evictions
 * 
 * Metrics exposed via Micrometer for Prometheus/Grafana integration.
 */
@Aspect
@Component
@Order(250)  // Run after cache logging interceptor (200)
@ConditionalOnProperty(name = "spring.redis.sentinel.enabled", havingValue = "true", matchIfMissing = false)
public class DistributedCacheMetrics {

    private static final Logger logger = LoggerFactory.getLogger(DistributedCacheMetrics.class);
    
    private final CacheManager cacheManager;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer cacheOperationTimer;

    public DistributedCacheMetrics(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        
        // Initialize metrics
        this.cacheHitCounter = Counter.builder("cache.distributed.hits")
                .description("Number of cache hits in distributed cache")
                .tag("type", "sentinel")
                .register(meterRegistry);
        
        this.cacheMissCounter = Counter.builder("cache.distributed.misses")
                .description("Number of cache misses in distributed cache")
                .tag("type", "sentinel")
                .register(meterRegistry);
        
        this.cacheOperationTimer = Timer.builder("cache.distributed.operation")
                .description("Time taken for cache operations")
                .tag("type", "sentinel")
                .register(meterRegistry);
        
        logger.info("Distributed cache metrics initialized");
    }

    /**
     * Monitor calculator operations for cache metrics.
     */
    @Around("execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.add(..)) || " +
            "execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.subtract(..)) || " +
            "execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.multiply(..)) || " +
            "execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.divide(..))")
    public Object collectMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        long startTime = System.nanoTime();
        
        try {
            // Generate cache key
            String cacheKey = generateCacheKey(methodName, args);
            
            // Check cache before method execution
            Cache cache = cacheManager.getCache("operations");
            
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null) {
                    cacheHitCounter.increment();
                    logger.debug("[METRIC] Cache HIT for key: {}", cacheKey);
                } else {
                    cacheMissCounter.increment();
                    logger.debug("[METRIC] Cache MISS for key: {}", cacheKey);
                }
            }
            
            // Execute method
            Object result = joinPoint.proceed();
            
            // Log metrics summary periodically
            logMetricsSummary();
            
            return result;
        } finally {
            // Record timing
            cacheOperationTimer.record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    private String generateCacheKey(String methodName, Object[] args) {
        StringBuilder key = new StringBuilder(methodName).append(":");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) key.append(":");
            key.append(args[i]);
        }
        return key.toString();
    }

    private void logMetricsSummary() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        if (total > 0 && total % 100 == 0) {  // Log every 100 operations
            double hitRate = (hits / total) * 100;
            logger.info("[METRICS] Cache Stats - Total: {}, Hits: {}, Misses: {}, Hit Rate: {:.2f}%", 
                    (long)total, (long)hits, (long)misses, hitRate);
        }
    }
}
