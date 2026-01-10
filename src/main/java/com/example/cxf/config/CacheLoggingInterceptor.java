package com.example.cxf.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspect to log cache operations (hits, misses, stores).
 * Runs AFTER Spring's caching aspect to properly detect cache hits.
 */
@Aspect
@Component
@Order(200) // Run after Spring's CacheInterceptor which is at order 1
public class CacheLoggingInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CacheLoggingInterceptor.class);
    
    private final CacheManager cacheManager;
    
    public CacheLoggingInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Around("execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.add(..)) || " +
            "execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.subtract(..)) || " +
            "execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.multiply(..)) || " +
            "execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.divide(..))")
    public Object logCacheOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Generate cache key based on method name and arguments
        String cacheKey = generateCacheKey(methodName, args);
        
        // Check if value exists in cache BEFORE method execution
        Cache cache = cacheManager.getCache("operations");
        boolean wasCached = false;
        Object cachedValue = null;
        
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                wasCached = true;
                cachedValue = wrapper.get();
            }
        }
        
        if (wasCached) {
            logger.info("[CACHE HIT] Method: {}(), Key: {}, Value: {}", 
                       methodName, cacheKey, cachedValue);
        } else {
            logger.info("[CACHE MISS] Method: {}(), Key: {}, Computing...", 
                       methodName, cacheKey);
        }
        
        // Execute the method (Spring's cache will handle caching)
        Object result = joinPoint.proceed();
        
        // If it was a miss, Spring has now cached it
        if (!wasCached) {
            logger.info("[CACHE STORED] Method: {}(), Key: {}, Value: {}", 
                       methodName, cacheKey, result);
        }
        
        return result;
    }

    @Around("execution(* com.example.cxf.soap.service.impl.CalculatorServiceImpl.performCalculation(..))")
    public Object logCalculationCacheOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        
        // args: [operand1, operand2, operation]
        int operand1 = (int) args[0];
        int operand2 = (int) args[1];
        String operation = ((String) args[2]).toUpperCase();
        
        // Generate cache key matching @Cacheable key
        String cacheKey = "calc:" + operand1 + ":" + operand2 + ":" + operation;
        
        // Check if value exists in cache BEFORE method execution
        Cache cache = cacheManager.getCache("calculations");
        boolean wasCached = false;
        Object cachedValue = null;
        
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                wasCached = true;
                cachedValue = wrapper.get();
            }
        }
        
        if (wasCached) {
            logger.info("[CACHE HIT] Method: calculate(), Key: {}, Value: {}", 
                       cacheKey, cachedValue);
        } else {
            logger.info("[CACHE MISS] Method: calculate(), Key: {}, Computing...", 
                       cacheKey);
        }
        
        // Execute the method (Spring's cache will handle caching)
        Object result = joinPoint.proceed();
        
        // If it was a miss, Spring has now cached it
        if (!wasCached) {
            logger.info("[CACHE STORED] Method: calculate(), Key: {}, Value: {}", 
                       cacheKey, result);
        }
        
        return result;
    }
    
    private String generateCacheKey(String methodName, Object[] args) {
        StringBuilder key = new StringBuilder(methodName).append(":");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) key.append(":");
            key.append(args[i]);
        }
        return key.toString();
    }
}
