package com.example.cxf.cache.distributed;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.stereotype.Component;

/**
 * Redis Sentinel Health Indicator.
 * 
 * Monitors:
 * - Redis connection status
 * - Master/Replica availability
 * - Sentinel health
 * 
 * Exposed via Spring Boot Actuator: /actuator/health/redisSentinel
 */
@Component("redisSentinelHealth")
@ConditionalOnProperty(name = "spring.redis.sentinel.enabled", havingValue = "true", matchIfMissing = false)
public class RedisSentinelHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelHealthIndicator.class);
    
    private final RedisConnectionFactory connectionFactory;

    public RedisSentinelHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            // Test Redis connection
            RedisServerCommands commands = connectionFactory.getConnection().serverCommands();
            Properties info = commands.info("replication");
            
            // Parse replication info
            String role = info.getProperty("role", "unknown");
            boolean isMaster = "master".equals(role);
            boolean isReplica = "slave".equals(role);
            
            Health.Builder builder = Health.up();
            
            if (isMaster) {
                builder.withDetail("role", "master");
                // Extract number of connected replicas
                String connectedSlaves = info.getProperty("connected_slaves", "0");
                builder.withDetail("connected_replicas", connectedSlaves);
            } else if (isReplica) {
                builder.withDetail("role", "replica");
                // Extract master connection status
                String masterLinkStatus = info.getProperty("master_link_status", "unknown");
                builder.withDetail("master_link_status", masterLinkStatus);
            }
            
            builder.withDetail("connection_type", "sentinel");
            builder.withDetail("status", "connected");
            
            logger.debug("Redis Sentinel health check: UP");
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Redis Sentinel health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("connection_type", "sentinel")
                    .build();
        }
    }
}
