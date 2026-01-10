# Distributed Cache Implementation Plan

## Current State
✅ **Already Implemented:**
- Redis single-node cache with Spring Boot
- Cache configuration with TTL (10 minutes)
- Cache logging (HIT/MISS/STORED)
- Manual caching for void methods (calculate)
- @Cacheable for return-value methods (add, subtract, multiply, divide)
- Cache management REST API

## Distributed Cache Goals

### 1. **High Availability (HA)**
- Redis Sentinel for automatic failover
- Master-slave replication
- Health monitoring

### 2. **Horizontal Scaling**
- Redis Cluster for data sharding
- Multiple nodes handling requests
- Load distribution

### 3. **Cache Synchronization**
- Consistent cache updates across nodes
- Cache invalidation strategies
- Event-driven cache updates

### 4. **Performance Optimization**
- Connection pooling
- Pipelining for batch operations
- Cache preloading/warming

---

## Implementation Options

### **Option 1: Redis Sentinel (Recommended for Start)**
**Best for:** High availability with automatic failover

**Pros:**
- Easier to set up than Redis Cluster
- Automatic failover and monitoring
- Good for small to medium deployments
- Works with existing Redis setup

**Cons:**
- Single master for writes (bottleneck at scale)
- Manual sharding if needed

**Architecture:**
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Sentinel 1 │     │  Sentinel 2 │     │  Sentinel 3 │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┴───────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
        ┌─────▼─────┐           ┌──────▼──────┐
        │  Master   │◄──────────┤   Replica   │
        │  Redis    │  Replicate│   Redis     │
        └───────────┘           └─────────────┘
```

**Implementation Steps:**
1. Install Redis Sentinel (3 instances)
2. Configure master-replica replication
3. Update Spring Boot config for Sentinel
4. Add connection retry logic
5. Implement health checks

---

### **Option 2: Redis Cluster**
**Best for:** Horizontal scaling and large deployments

**Pros:**
- Automatic sharding across nodes
- Distributed writes and reads
- Scales horizontally
- Built-in fault tolerance

**Cons:**
- More complex setup
- Some Redis commands not supported
- Requires minimum 3 master nodes

**Architecture:**
```
┌───────────┐  ┌───────────┐  ┌───────────┐
│ Master 1  │  │ Master 2  │  │ Master 3  │
│ (0-5460)  │  │ (5461-    │  │ (10923-   │
│           │  │  10922)   │  │  16383)   │
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │              │              │
┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
│ Replica 1 │  │ Replica 2 │  │ Replica 3 │
└───────────┘  └───────────┘  └───────────┘
```

---

### **Option 3: Hybrid (Local + Redis)**
**Best for:** Reducing Redis load and network latency

**Pros:**
- Fastest read performance (local cache)
- Reduced Redis load
- Automatic fallback to Redis

**Cons:**
- Cache consistency challenges
- Increased memory per instance
- Complex invalidation

**Architecture:**
```
┌─────────────────────────────────┐
│   Application Instance 1        │
│  ┌─────────────────────────┐   │
│  │  L1: Caffeine/Guava     │   │
│  └──────────┬──────────────┘   │
│             │                   │
│  ┌──────────▼──────────────┐   │
│  │  L2: Redis (Shared)     │   │
│  └─────────────────────────┘   │
└─────────────────────────────────┘
```

---

## Recommended Implementation Plan

### **Phase 1: Redis Sentinel Setup (Week 1)**
1. **Docker Compose Setup**
   - 1 Redis master
   - 2 Redis replicas
   - 3 Redis Sentinel instances
   
2. **Spring Boot Configuration**
   ```yaml
   spring:
     redis:
       sentinel:
         master: mymaster
         nodes:
           - localhost:26379
           - localhost:26380
           - localhost:26381
   ```

3. **Connection Pool Optimization**
   - Configure Lettuce connection pool
   - Set min/max connections
   - Configure timeouts

4. **Health Monitoring**
   - Add Redis health indicators
   - Implement circuit breaker
   - Add metrics (cache hit rate, latency)

### **Phase 2: Cache Synchronization (Week 2)**
1. **Cache Invalidation Strategy**
   - Time-based (TTL) ✅ Already implemented
   - Event-based (Redis Pub/Sub)
   - Manual via REST API ✅ Already implemented

2. **Redis Pub/Sub for Cache Events**
   - Publish cache invalidation events
   - Subscribe across instances
   - Handle distributed updates

3. **Cache Warming**
   - Pre-load frequently used data
   - Scheduled refresh
   - On-demand refresh

### **Phase 3: Monitoring & Observability (Week 3)**
1. **Metrics Collection**
   - Cache hit/miss rates
   - Response times
   - Memory usage
   - Eviction rates

2. **Distributed Tracing**
   - Track cache calls across services
   - Identify bottlenecks

3. **Alerting**
   - Cache unavailable
   - High miss rate
   - Memory pressure

---

## Docker Compose Example (Redis Sentinel)

```yaml
version: '3.8'

services:
  redis-master:
    image: redis:7-alpine
    container_name: redis-master
    ports:
      - "6379:6379"
    command: redis-server --requirepass yourpassword

  redis-replica-1:
    image: redis:7-alpine
    container_name: redis-replica-1
    ports:
      - "6380:6379"
    command: redis-server --replicaof redis-master 6379 --requirepass yourpassword --masterauth yourpassword

  redis-replica-2:
    image: redis:7-alpine
    container_name: redis-replica-2
    ports:
      - "6381:6379"
    command: redis-server --replicaof redis-master 6379 --requirepass yourpassword --masterauth yourpassword

  redis-sentinel-1:
    image: redis:7-alpine
    container_name: redis-sentinel-1
    ports:
      - "26379:26379"
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel1.conf:/etc/redis/sentinel.conf

  redis-sentinel-2:
    image: redis:7-alpine
    container_name: redis-sentinel-2
    ports:
      - "26380:26379"
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel2.conf:/etc/redis/sentinel.conf

  redis-sentinel-3:
    image: redis:7-alpine
    container_name: redis-sentinel-3
    ports:
      - "26381:26379"
    command: redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./sentinel3.conf:/etc/redis/sentinel.conf
```

---

## Key Decisions Needed

1. **Which option to start with?**
   - ✅ **Recommended: Option 1 (Redis Sentinel)** - Good balance of features and complexity

2. **Docker or Cloud?**
   - Docker for development/testing
   - Cloud Redis (AWS ElastiCache, Azure Cache) for production

3. **Cache Invalidation Strategy?**
   - TTL-based (current) ✅
   - Add Redis Pub/Sub for immediate invalidation
   - Keep REST API for manual control ✅

4. **Connection Pool Settings?**
   - Min: 5 connections
   - Max: 20 connections
   - Timeout: 2000ms

---

## Next Steps

1. **Discuss and decide** on implementation option
2. **Set up local environment** (Docker Compose)
3. **Update Spring Boot configuration**
4. **Implement failover handling**
5. **Add monitoring and metrics**
6. **Test failover scenarios**
7. **Document setup and operations**

---

## Testing Plan

### Failover Testing
- Kill master Redis → Verify Sentinel promotes replica
- Network partition → Verify recovery
- All replicas down → Verify graceful degradation

### Performance Testing
- Load test with cache hits/misses
- Measure latency with/without cache
- Test concurrent access

### Consistency Testing
- Multiple instances updating same cache
- Cache invalidation propagation
- Stale data scenarios

---

## Success Criteria

✅ Zero downtime during Redis master failure  
✅ Automatic failover in < 30 seconds  
✅ Cache hit rate > 80% for repeated operations  
✅ Response time < 50ms with cache hit  
✅ Graceful degradation when cache unavailable  
✅ Clear monitoring dashboards  
✅ Documented runbooks for operations  

---

**Ready to implement? Let's start with Phase 1: Redis Sentinel Setup!** 🚀
