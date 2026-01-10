# Enterprise Production-Grade Requirements

## Overview

This document outlines comprehensive requirements and enhancements needed to transform the ApacheCFXHelloWord application from a development prototype into an enterprise production-grade system. These enhancements cover security, observability, resilience, performance, testing, and operational excellence.

---

## Table of Contents

1. [Security](#1-security)
2. [Observability & Monitoring](#2-observability--monitoring)
3. [Resilience & Fault Tolerance](#3-resilience--fault-tolerance)
4. [Performance Optimization](#4-performance-optimization)
5. [Testing Strategy](#5-testing-strategy)
6. [API Versioning](#6-api-versioning)
7. [Configuration Management](#7-configuration-management)
8. [Database Integration](#8-database-integration)
9. [Message Queue Integration](#9-message-queue-integration)
10. [Container & Orchestration](#10-container--orchestration)
11. [CI/CD Pipeline](#11-cicd-pipeline)
12. [Documentation](#12-documentation)
13. [Compliance & Governance](#13-compliance--governance)
14. [Multi-tenancy](#14-multi-tenancy)
15. [API Gateway](#15-api-gateway)
16. [Implementation Roadmap](#16-implementation-roadmap)

---

## 1. Security 🔒

### 1.1 Authentication & Authorization

#### Requirements:
- **REST API Authentication**: JWT (JSON Web Tokens) or OAuth2 for stateless authentication
- **SOAP Authentication**: WS-Security with Username Token, SAML 2.0, or X.509 certificates
- **Role-Based Access Control (RBAC)**: Define roles (Admin, User, ReadOnly, etc.)
- **API Key Management**: For external integrations and third-party access

#### Implementation Details:

##### Spring Security Configuration
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

##### REST API - JWT Authentication
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/calculator/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );
        return http.build();
    }
}
```

##### SOAP - WS-Security Configuration
```java
@Configuration
public class SoapSecurityConfig {
    
    @Bean
    public Endpoint calculatorSecureEndpoint(Bus bus, CalculatorServiceImpl service) {
        EndpointImpl endpoint = new EndpointImpl(bus, service);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("ws-security.callback-handler", 
                      "com.example.cxf.security.PasswordCallbackHandler");
        properties.put("ws-security.username.token.validator",
                      "com.example.cxf.security.UsernameTokenValidator");
        
        endpoint.setProperties(properties);
        endpoint.publish("/SecureCalculator");
        return endpoint;
    }
}
```

#### User Roles & Permissions Matrix:

| Role | REST API Access | SOAP Access | Admin Functions |
|------|----------------|-------------|-----------------|
| ADMIN | Full | Full | Yes |
| USER | Read/Write | Read/Write | No |
| READONLY | Read Only | Read Only | No |
| SERVICE | API Key | API Key | No |

### 1.2 Security Headers & HTTPS

#### Requirements:
- **HTTPS/TLS**: All production traffic must use TLS 1.2+
- **Security Headers**: HSTS, X-Frame-Options, X-Content-Type-Options, CSP
- **CORS Configuration**: Restrict allowed origins in production
- **Rate Limiting**: Prevent abuse and DDoS attacks

#### Implementation:

##### HTTPS Configuration (application-prod.yml)
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
  http2:
    enabled: true
```

##### Security Headers Configuration
```java
@Configuration
public class WebSecurityConfig {
    
    @Bean
    public SecurityFilterChain securityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
            )
            .frameOptions().deny()
            .contentTypeOptions().disable()
            .xssProtection().block(true)
            .contentSecurityPolicy("default-src 'self'")
        );
        return http.build();
    }
}
```

##### CORS Configuration
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "https://prod.example.com",
            "https://api.example.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### 1.3 Input Validation & Sanitization

#### Requirements:
- **Bean Validation**: Use JSR-380 annotations
- **Custom Validators**: For complex business rules
- **SOAP Schema Validation**: Strict XSD validation
- **SQL Injection Prevention**: Parameterized queries only

#### Implementation:

```java
public class CalculationRequest {
    
    @NotNull(message = "Operand1 cannot be null")
    @Min(value = Integer.MIN_VALUE, message = "Operand1 out of range")
    @Max(value = Integer.MAX_VALUE, message = "Operand1 out of range")
    private Integer operand1;
    
    @NotNull(message = "Operand2 cannot be null")
    @Min(value = Integer.MIN_VALUE, message = "Operand2 out of range")
    @Max(value = Integer.MAX_VALUE, message = "Operand2 out of range")
    private Integer operand2;
    
    @NotBlank(message = "Operation cannot be blank")
    @Pattern(regexp = "^(ADD|SUBTRACT|MULTIPLY|DIVIDE)$", 
             message = "Invalid operation")
    private String operation;
}
```

### 1.4 Rate Limiting

#### Requirements:
- **Per-User Limits**: 100 requests/minute for authenticated users
- **Per-IP Limits**: 20 requests/minute for anonymous users
- **Global Limits**: 10,000 requests/minute system-wide

#### Implementation with Bucket4j:

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String key = getClientKey(httpRequest);
        
        Bucket bucket = cache.computeIfAbsent(key, k -> createBucket());
        
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.getWriter().write("Rate limit exceeded");
        }
    }
    
    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Limit.of(100, Duration.ofMinutes(1)))
            .build();
    }
}
```

---

## 2. Observability & Monitoring 📊

### 2.1 Metrics & Health Checks

#### Requirements:
- **Spring Boot Actuator**: Expose health, metrics, and info endpoints
- **Micrometer**: Collect application metrics
- **Prometheus Integration**: Export metrics in Prometheus format
- **Custom Business Metrics**: Track operation counts, success rates, error rates

#### Implementation:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

##### Application Configuration (application.yml)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

##### Custom Metrics
```java
@Service
public class CalculatorMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter addOperations;
    private final Counter divisionByZeroErrors;
    private final Timer calculationTimer;
    
    public CalculatorMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.addOperations = Counter.builder("calculator.operations")
            .tag("operation", "ADD")
            .description("Total ADD operations")
            .register(meterRegistry);
            
        this.divisionByZeroErrors = Counter.builder("calculator.errors")
            .tag("type", "DIVISION_BY_ZERO")
            .description("Division by zero errors")
            .register(meterRegistry);
            
        this.calculationTimer = Timer.builder("calculator.calculation.time")
            .description("Time taken for calculations")
            .register(meterRegistry);
    }
    
    public void recordOperation(String operation) {
        meterRegistry.counter("calculator.operations", "operation", operation).increment();
    }
    
    public <T> T timeCalculation(Supplier<T> calculation) {
        return calculationTimer.record(calculation);
    }
}
```

##### Health Check Indicators
```java
@Component
public class CalculatorHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Test basic calculation
            int result = 2 + 2;
            if (result == 4) {
                return Health.up()
                    .withDetail("calculator", "operational")
                    .withDetail("test", "2+2=4")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().withDetail("calculator", "test failed").build();
    }
}
```

#### Metrics to Track:

| Metric | Type | Purpose |
|--------|------|---------|
| `calculator.operations` | Counter | Total operations by type |
| `calculator.errors` | Counter | Errors by type |
| `calculator.calculation.time` | Timer | Calculation latency |
| `http.server.requests` | Timer | HTTP request latency |
| `jvm.memory.used` | Gauge | Memory usage |
| `jvm.gc.pause` | Timer | Garbage collection pauses |

### 2.2 Distributed Tracing

#### Requirements:
- **Spring Cloud Sleuth**: Automatic span creation
- **OpenTelemetry**: Industry-standard tracing
- **Jaeger/Zipkin**: Trace visualization
- **Correlation IDs**: End-to-end request tracking

#### Implementation:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 100% sampling in dev, 10% in prod
  zipkin:
    base-url: http://zipkin-server:9411
    sender:
      type: web
```

### 2.3 Advanced Logging

#### Requirements:
- **Structured JSON Logging**: Machine-parseable logs
- **Log Aggregation**: ELK Stack, Splunk, or Datadog
- **Async Logging**: Non-blocking log writes
- **Log Levels by Package**: Fine-grained control
- **Audit Logging**: Compliance and security

#### Implementation:

##### Logback Configuration (logback-spring.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- JSON Encoder for ELK Stack -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.json</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"calculator","env":"${SPRING_PROFILES_ACTIVE}"}</customFields>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.json.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- Async Appender for Performance -->
    <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <appender-ref ref="JSON_FILE"/>
    </appender>
    
    <!-- Different log levels per package -->
    <logger name="com.example.cxf" level="INFO"/>
    <logger name="com.example.cxf.soap" level="DEBUG"/>
    <logger name="org.apache.cxf" level="WARN"/>
    <logger name="org.springframework" level="INFO"/>
    
    <root level="INFO">
        <appender-ref ref="ASYNC_JSON"/>
    </root>
    
</configuration>
```

##### Audit Logging
```java
@Aspect
@Component
public class AuditLoggingAspect {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @Around("@annotation(auditable)")
    public Object auditMethodCall(ProceedingJoinPoint joinPoint, Auditable auditable) 
            throws Throwable {
        
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception error = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            AuditEvent event = AuditEvent.builder()
                .timestamp(Instant.now())
                .user(user)
                .method(method)
                .args(args)
                .result(result)
                .error(error)
                .duration(duration)
                .build();
                
            auditLogger.info("AUDIT: {}", event.toJson());
        }
    }
}
```

---

## 3. Resilience & Fault Tolerance 💪

### 3.1 Circuit Breaker Pattern

#### Requirements:
- **Automatic Failure Detection**: Detect failing services
- **Fast Failure**: Fail fast when service is down
- **Automatic Recovery**: Attempt recovery after timeout
- **Fallback Methods**: Graceful degradation

#### Implementation with Resilience4j:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      calculator:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
```

```java
@Service
public class ResilientCalculatorService {
    
    @CircuitBreaker(name = "calculator", fallbackMethod = "fallbackDivide")
    @Retry(name = "calculator")
    @TimeLimiter(name = "calculator")
    public CompletableFuture<Double> divideAsync(int a, int b) {
        return CompletableFuture.supplyAsync(() -> {
            if (b == 0) {
                throw new IllegalArgumentException("Division by zero");
            }
            return (double) a / b;
        });
    }
    
    private CompletableFuture<Double> fallbackDivide(int a, int b, Exception ex) {
        logger.warn("Fallback triggered for divide({}, {}): {}", a, b, ex.getMessage());
        return CompletableFuture.completedFuture(0.0);
    }
}
```

### 3.2 Retry & Timeout

```yaml
resilience4j:
  retry:
    instances:
      calculator:
        max-attempts: 3
        wait-duration: 1000
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          
  timelimiter:
    instances:
      calculator:
        timeout-duration: 5s
```

### 3.3 Bulkhead Pattern

```yaml
resilience4j:
  bulkhead:
    instances:
      calculator:
        max-concurrent-calls: 10
        max-wait-duration: 100ms
        
  thread-pool-bulkhead:
    instances:
      calculator:
        max-thread-pool-size: 4
        core-thread-pool-size: 2
        queue-capacity: 2
```

---

## 4. Performance Optimization ⚡

### 4.1 Caching Strategy

#### Requirements:
- **Redis Integration**: Distributed caching
- **Caffeine**: In-memory caching for frequently accessed data
- **Cache Eviction Policies**: LRU, TTL-based
- **Cache Monitoring**: Hit/miss rates

#### Implementation:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      ssl: true
```

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
                
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

```java
@Service
public class CachedCalculatorService {
    
    @Cacheable(value = "calculations", key = "#a + '-' + #b + '-' + #operation")
    public double calculate(int a, int b, String operation) {
        // Expensive calculation cached for 10 minutes
        return performCalculation(a, b, operation);
    }
    
    @CacheEvict(value = "calculations", allEntries = true)
    @Scheduled(fixedRate = 3600000) // Clear cache hourly
    public void evictAllCaches() {
        logger.info("Evicting all calculation caches");
    }
}
```

### 4.2 Database Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: CalculatorHikariCP
      
      # Connection test query
      connection-test-query: SELECT 1
      
      # Leak detection
      leak-detection-threshold: 60000
```

### 4.3 Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("calc-async-");
        executor.initialize();
        return executor;
    }
}
```

```java
@Service
public class AsyncCalculatorService {
    
    @Async("taskExecutor")
    public CompletableFuture<Double> calculateAsync(int a, int b, String op) {
        // Non-blocking calculation
        double result = performCalculation(a, b, op);
        return CompletableFuture.completedFuture(result);
    }
}
```

---

## 5. Testing Strategy 🧪

### 5.1 Unit Tests

#### Requirements:
- **Code Coverage**: Minimum 80% line coverage
- **JUnit 5**: Modern testing framework
- **Mockito**: Mocking dependencies
- **AssertJ**: Fluent assertions

#### Test Structure:

```
src/test/java/com/example/cxf/
├── logging/
│   ├── ErrorCodeTest.java
│   ├── LoggerUtilTest.java
│   └── TransactionIdFilterTest.java
├── soap/
│   ├── config/
│   │   └── CalculatorConfigTest.java
│   └── service/
│       └── impl/
│           └── CalculatorServiceImplTest.java
└── rest/
    └── controller/
        └── CalculatorRestControllerTest.java
```

#### Example Unit Test:

```java
@ExtendWith(MockitoExtension.class)
class CalculatorServiceImplTest {
    
    @Mock
    private Logger logger;
    
    @InjectMocks
    private CalculatorServiceImpl service;
    
    @Test
    @DisplayName("Should add two numbers correctly")
    void testAdd() {
        // Given
        int a = 10, b = 5;
        
        // When
        int result = service.add(a, b);
        
        // Then
        assertThat(result).isEqualTo(15);
    }
    
    @Test
    @DisplayName("Should throw exception on division by zero")
    void testDivisionByZero() {
        // Given
        int a = 10, b = 0;
        
        // When/Then
        assertThatThrownBy(() -> service.divide(a, b))
            .isInstanceOf(ServiceFailoverFault_Exception.class)
            .hasMessageContaining("Division by zero");
    }
    
    @ParameterizedTest
    @CsvSource({
        "10, 5, 15",
        "-10, 5, -5",
        "0, 0, 0",
        "100, -50, 50"
    })
    @DisplayName("Should handle various add operations")
    void testAddWithMultipleInputs(int a, int b, int expected) {
        assertThat(service.add(a, b)).isEqualTo(expected);
    }
}
```

### 5.2 Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CalculatorRestControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "USER")
    void testAddEndpoint() throws Exception {
        mockMvc.perform(get("/api/calculator/add")
                .param("a", "10")
                .param("b", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value(15))
            .andExpect(header().exists("X-Transaction-ID"));
    }
}
```

### 5.3 Performance Tests

```java
@Test
@Tag("performance")
void testCalculationPerformance() {
    long startTime = System.nanoTime();
    
    for (int i = 0; i < 10000; i++) {
        service.add(i, i + 1);
    }
    
    long duration = System.nanoTime() - startTime;
    long avgTimePerOp = duration / 10000;
    
    assertThat(avgTimePerOp)
        .as("Average time per operation")
        .isLessThan(1000); // Less than 1 microsecond
}
```

---

## 6. API Versioning 🔢

### Requirements:
- **URI Versioning**: `/api/v1/`, `/api/v2/`
- **Header Versioning**: `Accept: application/vnd.example.v1+json`
- **SOAP Versioning**: Multiple WSDL versions
- **Backward Compatibility**: Support N-1 versions

### Implementation:

```java
// v1 Controller
@RestController
@RequestMapping("/api/v1/calculator")
public class CalculatorRestControllerV1 {
    
    @GetMapping("/add")
    public ResponseEntity<Integer> add(@RequestParam int a, @RequestParam int b) {
        return ResponseEntity.ok(a + b);
    }
}

// v2 Controller with enhanced response
@RestController
@RequestMapping("/api/v2/calculator")
public class CalculatorRestControllerV2 {
    
    @GetMapping("/add")
    public ResponseEntity<CalculationResponseV2> add(@RequestParam int a, @RequestParam int b) {
        CalculationResponseV2 response = CalculationResponseV2.builder()
            .result(a + b)
            .operands(List.of(a, b))
            .operation("ADD")
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.ok(response);
    }
}
```

---

## 7. Configuration Management ⚙️

### Requirements:
- **Spring Cloud Config Server**: Centralized configuration
- **Environment-Specific Profiles**: dev, staging, prod
- **Encrypted Secrets**: Vault, AWS Secrets Manager, Azure Key Vault
- **Feature Flags**: Toggle features without deployment

### Implementation:

```yaml
# application.yml (common)
spring:
  application:
    name: calculator-service
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true
      retry:
        max-attempts: 6

# application-dev.yml
logging:
  level:
    com.example.cxf: DEBUG
calculator:
  rate-limit: 1000

# application-prod.yml
logging:
  level:
    com.example.cxf: INFO
calculator:
  rate-limit: 100
```

### Feature Flags with Togglz:

```xml
<dependency>
    <groupId>org.togglz</groupId>
    <artifactId>togglz-spring-boot-starter</artifactId>
    <version>4.0.0</version>
</dependency>
```

```java
public enum Features implements Feature {
    
    @Label("Enable Advanced Calculations")
    ADVANCED_CALCULATIONS,
    
    @Label("Enable Caching")
    ENABLE_CACHING,
    
    @Label("Enable Rate Limiting")
    RATE_LIMITING;
    
    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
```

---

## 8. Database Integration 💾

### Requirements:
- **Spring Data JPA**: ORM for database operations
- **Calculation History**: Store all calculations with timestamps
- **Audit Trail**: Who did what when
- **User Preferences**: Store user-specific settings

### Schema Design:

```sql
-- Calculation History
CREATE TABLE calculations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    operation VARCHAR(20) NOT NULL,
    operand1 INT NOT NULL,
    operand2 INT NOT NULL,
    result DOUBLE PRECISION NOT NULL,
    transaction_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_transaction_id (transaction_id)
);

-- Audit Trail
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- User Preferences
CREATE TABLE user_preferences (
    user_id VARCHAR(255) PRIMARY KEY,
    default_operation VARCHAR(20),
    decimal_precision INT DEFAULT 2,
    notification_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### JPA Entities:

```java
@Entity
@Table(name = "calculations")
@Data
@Builder
public class Calculation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Operation operation;
    
    private Integer operand1;
    private Integer operand2;
    private Double result;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
```

---

## 9. Message Queue Integration 📨

### Requirements:
- **Async Processing**: Handle long-running calculations
- **Event-Driven Architecture**: Publish calculation events
- **Dead Letter Queue**: Handle failed messages
- **Message Retry**: Automatic retry with backoff

### Implementation with RabbitMQ:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```yaml
spring:
  rabbitmq:
    host: rabbitmq-server
    port: 5672
    username: ${RABBITMQ_USER}
    password: ${RABBITMQ_PASSWORD}
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2
```

```java
@Configuration
public class RabbitMQConfig {
    
    public static final String CALCULATION_QUEUE = "calculation.queue";
    public static final String CALCULATION_EXCHANGE = "calculation.exchange";
    public static final String CALCULATION_ROUTING_KEY = "calculation.routing.key";
    
    @Bean
    public Queue calculationQueue() {
        return QueueBuilder.durable(CALCULATION_QUEUE)
            .withArgument("x-dead-letter-exchange", "calculation.dlx")
            .withArgument("x-message-ttl", 300000) // 5 minutes
            .build();
    }
    
    @Bean
    public TopicExchange calculationExchange() {
        return new TopicExchange(CALCULATION_EXCHANGE);
    }
    
    @Bean
    public Binding binding() {
        return BindingBuilder
            .bind(calculationQueue())
            .to(calculationExchange())
            .with(CALCULATION_ROUTING_KEY);
    }
}
```

```java
@Service
public class CalculationProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishCalculation(CalculationEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.CALCULATION_EXCHANGE,
            RabbitMQConfig.CALCULATION_ROUTING_KEY,
            event
        );
    }
}

@Service
public class CalculationConsumer {
    
    @RabbitListener(queues = RabbitMQConfig.CALCULATION_QUEUE)
    public void handleCalculation(CalculationEvent event) {
        logger.info("Processing calculation: {}", event);
        // Process async calculation
    }
}
```

---

## 10. Container & Orchestration 🐳

### 10.1 Docker Configuration

#### Multi-Stage Dockerfile:

```dockerfile
# Build stage
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from build stage
COPY --from=build /app/target/ApacheCFXHelloWord.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose for Local Development:

```yaml
version: '3.8'

services:
  calculator:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - REDIS_HOST=redis
      - RABBITMQ_HOST=rabbitmq
    depends_on:
      - redis
      - rabbitmq
      - postgres
    networks:
      - calculator-network
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - calculator-network
      
  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"
    networks:
      - calculator-network
      
  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=calculator
      - POSTGRES_USER=calculator
      - POSTGRES_PASSWORD=calculator123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - calculator-network

networks:
  calculator-network:
    driver: bridge

volumes:
  postgres-data:
```

### 10.2 Kubernetes Deployment

#### Deployment Manifest:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: calculator-service
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: calculator
  template:
    metadata:
      labels:
        app: calculator
        version: v1
    spec:
      containers:
      - name: calculator
        image: calculator-service:1.0.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: calculator-secrets
              key: redis-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: calculator-service
  namespace: production
spec:
  selector:
    app: calculator
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: calculator-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: calculator-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 11. CI/CD Pipeline 🚀

### GitHub Actions Workflow:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn clean package
    
    - name: Run tests
      run: mvn test
    
    - name: Code coverage
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
    
    - name: SonarQube Scan
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      run: mvn sonar:sonar
    
    - name: Security scan with Snyk
      uses: snyk/actions/maven@master
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
  
  docker-build:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Login to DockerHub
      uses: docker/login-action@v2
      with:
        username: ${{ secrets.DOCKERHUB_USERNAME }}
        password: ${{ secrets.DOCKERHUB_TOKEN }}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: calculator-service:${{ github.sha }},calculator-service:latest
  
  deploy-to-k8s:
    needs: docker-build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG }}
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/calculator-service \
          calculator=calculator-service:${{ github.sha }} \
          -n production
        kubectl rollout status deployment/calculator-service -n production
```

---

## 12. Documentation 📚

### Requirements:
- **Architecture Decision Records (ADR)**: Document key decisions
- **API Changelog**: Track API changes
- **Runbooks**: Operational procedures
- **Disaster Recovery**: Recovery procedures
- **SLA Definitions**: Service level agreements

### ADR Template:

```markdown
# ADR-001: Use Redis for Distributed Caching

## Status
Accepted

## Context
Need distributed caching for calculator operations across multiple instances.
Requirements: High performance, TTL support, cluster mode.

## Decision
Use Redis as distributed cache solution.

## Consequences
**Positive:**
- Sub-millisecond latency
- Native TTL support
- Production-proven
- Good Spring Boot integration

**Negative:**
- Additional infrastructure component
- Requires monitoring
- Network dependency

## Alternatives Considered
- Hazelcast
- Memcached
- Caffeine (in-memory only)
```

---

## 13. Compliance & Governance 📋

### Requirements:
- **GDPR Compliance**: Data privacy and right to be forgotten
- **PCI DSS**: If handling payment data
- **SOC 2**: Security and availability
- **Data Retention**: Automatic data deletion policies

### Implementation:

```java
@Service
public class DataRetentionService {
    
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void deleteOldCalculations() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        int deleted = calculationRepository.deleteByCreatedAtBefore(cutoff);
        logger.info("Deleted {} old calculations (older than 90 days)", deleted);
    }
}
```

---

## 14. Multi-tenancy 🏬

### Requirements:
- **Tenant Isolation**: Data segregation per client
- **Per-Tenant Configuration**: Custom settings per tenant
- **Tenant-Specific Rate Limits**: Fair usage policies
- **Tenant Metrics**: Per-tenant monitoring

### Implementation:

```java
@Component
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getTenantId() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}

@Component
public class TenantFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = httpRequest.getHeader("X-Tenant-ID");
        
        try {
            TenantContext.setTenantId(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

---

## 15. API Gateway 🌐

### Requirements:
- **Centralized Routing**: Single entry point
- **Load Balancing**: Distribute traffic
- **Request Transformation**: Modify requests/responses
- **API Composition**: Combine multiple APIs

### Spring Cloud Gateway Configuration:

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: calculator-rest
        uri: lb://calculator-service
        predicates:
        - Path=/api/**
        filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
        - AddRequestHeader=X-Gateway, SpringCloudGateway
        
      - id: calculator-soap
        uri: lb://calculator-service
        predicates:
        - Path=/services/**
        filters:
        - AddRequestHeader=X-Protocol, SOAP
```

---

## 16. Implementation Roadmap

### Phase 1: Immediate (1-2 weeks) - Foundation

| Priority | Item | Effort | Dependencies |
|----------|------|--------|--------------|
| P0 | Spring Boot Actuator | 1 day | None |
| P0 | Docker Configuration | 2 days | None |
| P0 | Unit Tests (50% coverage) | 3 days | None |
| P1 | Spring Security (Basic Auth) | 2 days | None |
| P1 | Structured JSON Logging | 1 day | None |
| P2 | CI/CD Pipeline (GitHub Actions) | 2 days | Docker |

**Deliverables:**
- Health endpoints available
- Docker image published
- Basic security in place
- Automated build pipeline

### Phase 2: Short-term (1 month) - Resilience

| Priority | Item | Effort | Dependencies |
|----------|------|--------|--------------|
| P0 | Resilience4j (Circuit Breaker) | 3 days | Actuator |
| P0 | Redis Caching | 2 days | Docker |
| P1 | Integration Tests | 4 days | Unit Tests |
| P1 | API Versioning | 2 days | None |
| P2 | Prometheus Metrics | 2 days | Actuator |
| P2 | Database Integration | 5 days | None |

**Deliverables:**
- Circuit breakers active
- Caching layer operational
- 80%+ test coverage
- Production monitoring ready

### Phase 3: Long-term (3-6 months) - Production Scale

| Priority | Item | Effort | Dependencies |
|----------|------|--------|--------------|
| P0 | Kubernetes Deployment | 1 week | Docker, DB |
| P0 | Distributed Tracing | 3 days | Actuator |
| P1 | Message Queue Integration | 1 week | DB |
| P1 | API Gateway | 1 week | K8s |
| P2 | Multi-tenancy | 2 weeks | DB, Security |
| P2 | Advanced Security (OAuth2) | 1 week | Security |

**Deliverables:**
- Production Kubernetes cluster
- End-to-end tracing
- Event-driven architecture
- Enterprise-grade security

---

## Summary

This document provides a comprehensive roadmap for transforming the Calculator service into an enterprise production-grade application. The recommendations are prioritized based on:

1. **Security**: Protect data and prevent unauthorized access
2. **Reliability**: Ensure service availability and fault tolerance
3. **Observability**: Monitor, trace, and debug effectively
4. **Performance**: Optimize for scale and efficiency
5. **Maintainability**: Test thoroughly and document well

**Next Steps:**
1. Review and prioritize requirements with stakeholders
2. Estimate effort and timeline for Phase 1
3. Begin implementation starting with Spring Boot Actuator
4. Set up CI/CD pipeline early for continuous delivery
5. Iteratively add features from Phase 2 and Phase 3

**Success Metrics:**
- 99.9% uptime SLA
- <100ms p95 latency
- 80%+ test coverage
- Zero critical security vulnerabilities
- Automated deployments

---

**Document Version**: 1.0  
**Last Updated**: January 10, 2026  
**Author**: Development Team  
**Status**: Draft - Pending Review
