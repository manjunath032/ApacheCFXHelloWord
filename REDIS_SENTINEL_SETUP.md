# Redis Sentinel Distributed Cache Setup

## Overview

This document provides complete instructions for setting up and testing Redis Sentinel for distributed caching in the Calculator application.

## Architecture

### Components

1. **Redis Master** (port 6379)
   - Primary read/write node
   - Handles all write operations
   - Serves read operations

2. **Redis Replicas** (ports 6380, 6381)
   - Two replica nodes
   - Asynchronous replication from master
   - Read-only operations (when configured)

3. **Redis Sentinels** (ports 26379, 26380, 26381)
   - Three sentinel nodes for monitoring
   - Quorum: 2 (minimum sentinels to agree on failover)
   - Automatic failover management
   - Configuration distribution

### High Availability Features

- **Automatic Failover**: If master fails, sentinels elect a new master from replicas
- **Configuration Provider**: Sentinels provide current master address to clients
- **Monitoring**: Continuous health checks on all Redis nodes
- **Notification**: Alerts via logs when topology changes occur

## Prerequisites

1. **Docker Desktop** installed and running
2. **Java 21** (for running the application)
3. **Maven** (for building the application)

## Quick Start

### 1. Start Redis Sentinel Cluster

```powershell
# Navigate to docker directory
cd f:\Projects\ApacheCFXHelloWord\docker

# Start all containers
docker-compose -f docker-compose-sentinel.yml up -d

# Verify all containers are running
docker-compose -f docker-compose-sentinel.yml ps
```

Expected output:
```
NAME                   STATUS    PORTS
redis-master           Up        0.0.0.0:6379->6379/tcp
redis-replica-1        Up        0.0.0.0:6380->6379/tcp
redis-replica-2        Up        0.0.0.0:6381->6379/tcp
redis-sentinel-1       Up        0.0.0.0:26379->26379/tcp
redis-sentinel-2       Up        0.0.0.0:26380->26379/tcp
redis-sentinel-3       Up        0.0.0.0:26381->26379/tcp
```

### 2. Verify Sentinel Configuration

```powershell
# Check sentinel status
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL masters

# Check master info
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL master mymaster

# Check replica info
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster

# Check sentinel info
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL sentinels mymaster
```

### 3. Run Application with Sentinel Profile

```powershell
cd f:\Projects\ApacheCFXHelloWord

# Build the application
mvn clean package -DskipTests

# Run with Sentinel profile
java -jar target\ApacheCFXHelloWord-1.0.0.jar --spring.profiles.active=sentinel
```

### 4. Verify Application Health

Open browser and check:
- Health endpoint: http://localhost:8080/actuator/health
- Metrics endpoint: http://localhost:8080/actuator/metrics

Expected health response:
```json
{
  "status": "UP",
  "components": {
    "redisSentinel": {
      "status": "UP",
      "details": {
        "role": "master",
        "connected_replicas": "2",
        "connection_type": "sentinel",
        "status": "connected"
      }
    }
  }
}
```

## Configuration Profiles

### Default Profile (Single Node)
```yaml
spring:
  profiles:
    active: default
  data:
    redis:
      host: localhost
      port: 6379
```

### Sentinel Profile (High Availability)
```yaml
spring:
  profiles:
    active: sentinel
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - localhost:26379
          - localhost:26380
          - localhost:26381
```

Switch profiles using:
```powershell
# Default single-node
java -jar target\ApacheCFXHelloWord-1.0.0.jar

# Sentinel cluster
java -jar target\ApacheCFXHelloWord-1.0.0.jar --spring.profiles.active=sentinel
```

## Testing

### Test Cache Operations

```powershell
# Test multiply operation (should cache)
Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" `
  -Method POST `
  -ContentType "text/xml" `
  -InFile "test-requests\calculator-multiply.xml"

# Check application logs for cache activity
# You should see:
# [CACHE MISS] Key: ...
# [CACHE STORED] Key: ...
# [CACHE HIT] Key: ... (on second call)
```

### Test Failover Scenario

#### 1. Baseline Test
```powershell
# Make initial request
Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" `
  -Method POST -ContentType "text/xml" `
  -InFile "test-requests\calculator-multiply.xml"
```

#### 2. Simulate Master Failure
```powershell
# Stop the master
docker stop redis-master

# Wait 10-15 seconds for failover
Start-Sleep -Seconds 15

# Check new master (should be replica-1 or replica-2)
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
```

#### 3. Verify Application Still Works
```powershell
# Make request again (should still work with new master)
Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" `
  -Method POST -ContentType "text/xml" `
  -InFile "test-requests\calculator-multiply.xml"

# Cache should still work
# Check logs for [CACHE HIT] message
```

#### 4. Restore Original Master
```powershell
# Start original master (will become replica)
docker start redis-master

# Wait for synchronization
Start-Sleep -Seconds 10

# Verify topology
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster
```

### Test Connection Pool

```powershell
# Create test script for concurrent requests
$script = @"
1..100 | ForEach-Object -Parallel {
    Invoke-WebRequest -Uri "http://localhost:8080/services/CalculatorService" ``
      -Method POST -ContentType "text/xml" ``
      -InFile "test-requests\calculator-multiply.xml"
} -ThrottleLimit 10
"@

$script | Out-File -FilePath "test-connection-pool.ps1"

# Run concurrent test
.\test-connection-pool.ps1

# Check metrics
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/cache.distributed.hits"
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/cache.distributed.misses"
```

## Monitoring

### Health Checks

```powershell
# Application health
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"

# Redis Sentinel health specifically
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health/redisSentinel"
```

### Metrics

Available metrics:
- `cache.distributed.hits` - Cache hit counter
- `cache.distributed.misses` - Cache miss counter
- `cache.distributed.operation` - Operation latency timer

```powershell
# View cache hits
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/cache.distributed.hits"

# View cache misses
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/cache.distributed.misses"

# View operation timing
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/cache.distributed.operation"
```

### Docker Container Logs

```powershell
# View master logs
docker logs redis-master

# View sentinel logs
docker logs redis-sentinel-1

# Follow sentinel logs in real-time
docker logs -f redis-sentinel-1

# View all logs
docker-compose -f docker-compose-sentinel.yml logs
```

### Redis CLI Monitoring

```powershell
# Connect to master and monitor commands
docker exec -it redis-master redis-cli MONITOR

# Check replication info
docker exec redis-master redis-cli INFO replication

# Check sentinel info
docker exec redis-sentinel-1 redis-cli -p 26379 INFO sentinel
```

## Troubleshooting

### Issue: Application Can't Connect to Sentinel

**Symptoms**: Application fails to start with connection errors

**Solutions**:
1. Verify sentinels are running:
   ```powershell
   docker-compose -f docker-compose-sentinel.yml ps
   ```

2. Check sentinel configuration:
   ```powershell
   docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL masters
   ```

3. Verify network connectivity:
   ```powershell
   Test-NetConnection localhost -Port 26379
   Test-NetConnection localhost -Port 26380
   Test-NetConnection localhost -Port 26381
   ```

### Issue: Failover Not Working

**Symptoms**: Master failure doesn't trigger failover

**Solutions**:
1. Check sentinel quorum (should be 2):
   ```powershell
   docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL master mymaster | Select-String "quorum"
   ```

2. Verify at least 2 sentinels are running:
   ```powershell
   docker-compose -f docker-compose-sentinel.yml ps | Select-String "sentinel"
   ```

3. Check sentinel logs for errors:
   ```powershell
   docker logs redis-sentinel-1
   ```

### Issue: Cache Not Working

**Symptoms**: All requests show [CACHE MISS]

**Solutions**:
1. Verify Redis connection:
   ```powershell
   docker exec redis-master redis-cli PING
   ```

2. Check cache configuration in logs:
   ```
   Look for: "Initialized RedisCacheManager with custom configurations"
   ```

3. Verify profile is active:
   ```
   Look for: "The following profiles are active: sentinel"
   ```

### Issue: High Memory Usage

**Symptoms**: Redis containers consuming too much memory

**Solutions**:
1. Check cache size:
   ```powershell
   docker exec redis-master redis-cli INFO memory
   ```

2. Clear cache if needed:
   ```powershell
   docker exec redis-master redis-cli FLUSHDB
   ```

3. Reduce TTL in `application.yml`:
   ```yaml
   cache:
     redis:
       time-to-live: 300000  # Reduce from 600000 to 300000 (5 minutes)
   ```

## Performance Tuning

### Connection Pool Settings

Current configuration in `RedisSentinelConfig.java`:
```java
poolConfig.setMaxTotal(20);      // Max connections
poolConfig.setMaxIdle(10);       // Max idle connections
poolConfig.setMinIdle(5);        // Min idle connections
poolConfig.setMaxWaitMillis(3000); // Max wait for connection
```

Adjust based on load:
- **Low traffic** (< 10 req/s): maxTotal=10, maxIdle=5, minIdle=2
- **Medium traffic** (10-50 req/s): maxTotal=20, maxIdle=10, minIdle=5 (current)
- **High traffic** (> 50 req/s): maxTotal=50, maxIdle=25, minIdle=10

### Cache TTL Settings

Current TTL: 10 minutes (600000ms)

Adjust in `application.yml`:
```yaml
cache:
  redis:
    time-to-live: 600000  # Adjust based on data volatility
```

Recommendations:
- **Highly volatile data**: 1-5 minutes
- **Moderate volatility**: 5-15 minutes (current: 10)
- **Stable data**: 30-60 minutes

### Read Strategy

Current: `ReadFrom.REPLICA_PREFERRED`

Other options in `RedisSentinelConfig.java`:
- `ReadFrom.MASTER`: All reads from master (higher consistency)
- `ReadFrom.MASTER_PREFERRED`: Prefer master, fallback to replica
- `ReadFrom.REPLICA`: All reads from replica (higher throughput)
- `ReadFrom.REPLICA_PREFERRED`: Prefer replica, fallback to master (current)

## Maintenance

### Backup Data

```powershell
# Trigger save on master
docker exec redis-master redis-cli BGSAVE

# Copy RDB file from container
docker cp redis-master:/data/dump.rdb ./backup-$(Get-Date -Format 'yyyyMMdd-HHmmss').rdb
```

### Update Configuration

```powershell
# Stop containers
docker-compose -f docker-compose-sentinel.yml down

# Edit configuration files in docker/redis-sentinel/

# Restart containers
docker-compose -f docker-compose-sentinel.yml up -d
```

### Clean Up

```powershell
# Stop and remove containers
docker-compose -f docker-compose-sentinel.yml down

# Remove volumes (WARNING: deletes all data)
docker-compose -f docker-compose-sentinel.yml down -v

# Remove images
docker rmi redis:7-alpine
```

## Security Considerations

### Current Setup (Development)
- ⚠️ No authentication
- ⚠️ Exposed ports on localhost
- ⚠️ No encryption

### Production Recommendations

1. **Enable Authentication**:
   ```yaml
   # In docker-compose-sentinel.yml
   command: redis-server --requirepass YOUR_PASSWORD
   ```

2. **Use TLS**:
   ```yaml
   # In application.yml
   spring:
     data:
       redis:
         ssl: true
   ```

3. **Network Isolation**:
   - Use Docker network without port mapping
   - Access via reverse proxy

4. **Firewall Rules**:
   - Restrict sentinel ports (26379-26381)
   - Allow only application servers

## Next Steps

After successful setup:

1. **Phase 2: Cache Synchronization**
   - Implement Redis Pub/Sub for cache invalidation
   - Add cache warming strategies
   - Create event-driven synchronization

2. **Phase 3: Advanced Monitoring**
   - Set up Prometheus for metrics collection
   - Create Grafana dashboards
   - Implement alerting rules

3. **Load Testing**
   - Test with concurrent requests
   - Measure failover time
   - Validate cache hit ratio

4. **Documentation**
   - Create runbook for operations team
   - Document incident response procedures
   - Create architecture diagrams

## References

- [Redis Sentinel Documentation](https://redis.io/docs/management/sentinel/)
- [Spring Data Redis Documentation](https://spring.io/projects/spring-data-redis)
- [Lettuce Documentation](https://lettuce.io/core/release/reference/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## Support

For issues or questions:
1. Check application logs in `logs/application.log`
2. Review Docker container logs
3. Verify sentinel status with `SENTINEL masters` command
4. Test connectivity with `redis-cli PING`

---

**Last Updated**: 2026-01-10
**Version**: 1.0.0
**Author**: Development Team
