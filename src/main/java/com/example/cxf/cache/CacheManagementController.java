package com.example.cxf.cache;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for Cache Management.
 * Provides endpoints to monitor and manage Redis caches.
 */
@RestController
@RequestMapping("/api/cache")
@Tag(name = "Cache Management", description = "Monitor and manage Redis caches")
public class CacheManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CacheManagementController.class);

    @Autowired
    private CacheManager cacheManager;

    @GetMapping("/info")
    @Operation(
        summary = "Get cache information",
        description = "Returns information about all configured caches"
    )
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        logger.debug("Getting cache information");
        
        Map<String, Object> response = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();
        
        response.put("totalCaches", cacheNames.size());
        response.put("cacheNames", cacheNames);
        response.put("cacheManager", cacheManager.getClass().getSimpleName());
        
        logger.info("Cache info retrieved: {} caches found", cacheNames.size());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear/{cacheName}")
    @Operation(
        summary = "Clear specific cache",
        description = "Clears all entries from the specified cache"
    )
    public ResponseEntity<Map<String, Object>> clearCache(
            @Parameter(description = "Name of the cache to clear", example = "operations")
            @PathVariable String cacheName
    ) {
        logger.info("Attempting to clear cache: {}", cacheName);
        
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            logger.warn("Cache not found: {}", cacheName);
            return ResponseEntity.notFound().build();
        }
        
        cache.clear();
        logger.info("Cache cleared successfully: {}", cacheName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache '" + cacheName + "' cleared successfully");
        response.put("cacheName", cacheName);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear-all")
    @Operation(
        summary = "Clear all caches",
        description = "Clears all entries from all caches"
    )
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        logger.info("Attempting to clear all caches");
        
        Collection<String> cacheNames = cacheManager.getCacheNames();
        List<String> clearedCaches = new ArrayList<>();
        
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCaches.add(cacheName);
                logger.debug("Cache cleared: {}", cacheName);
            }
        }
        
        logger.info("All caches cleared successfully. Total: {}", clearedCaches.size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All caches cleared successfully");
        response.put("totalCleared", clearedCaches.size());
        response.put("clearedCaches", clearedCaches);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Get cache statistics",
        description = "Returns statistics about cache usage (if supported by cache provider)"
    )
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        logger.debug("Getting cache statistics");
        
        Map<String, Object> response = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();
        Map<String, String> cacheInfo = new HashMap<>();
        
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cacheInfo.put(cacheName, "Active");
            }
        }
        
        response.put("caches", cacheInfo);
        response.put("totalCaches", cacheNames.size());
        
        logger.info("Cache statistics retrieved");
        return ResponseEntity.ok(response);
    }
}
