# 🚀 Enterprise-Grade Upgrade Plan

## 📋 Tổng Quan

Nâng cấp hệ thống Trading AI từ MVP lên **Enterprise-Grade Production-Ready System**.

**Mục tiêu:** Đạt 10/10 trên tất cả tiêu chí enterprise (Type Safety, Data Integrity, Auditability, Validation, Scalability)

---

## ✅ PHASE 1: CRITICAL FIXES (Production Blockers)

### 1.1 Unique Constraint cho Candle Entity
**File:** `src/main/java/fpt/wongun/trading_ai/domain/entity/Candle.java`

**Vấn đề:**
- Có thể insert duplicate candle (cùng symbol + timeframe + timestamp)
- Scheduler chạy 2 lần → duplicate data
- Database phình to không cần thiết

**Solution:**
```java
@Table(name = "candles",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_candle_symbol_timeframe_timestamp",
            columnNames = {"symbol_id", "timeframe", "timestamp"}
        )
    },
    indexes = {
        @Index(name = "idx_candle_symbol_tf_time", 
               columnList = "symbol_id,timeframe,timestamp")
    })
```

**Impact:** 🔴 Critical - Prevents data corruption

---

### 1.2 Validation Annotations cho All Entities
**Files:**
- `Symbol.java`
- `Candle.java`
- `AiSignal.java`

**Vấn đề:**
- Chỉ có DB-level validation
- API errors không clear
- Fail late (ở DB layer thay vì controller layer)

**Solution:**

#### Symbol.java
```java
@NotBlank(message = "Symbol code is required")
@Pattern(regexp = "^[A-Z]{3,10}$", message = "Symbol code must be 3-10 uppercase letters")
@Column(nullable = false, unique = true, length = 50)
private String code;

@NotNull(message = "Symbol type is required")
@Enumerated(EnumType.STRING)
private SymbolType type;
```

#### Candle.java
```java
@NotNull(message = "Open price is required")
@DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
@Column(nullable = false, precision = 28, scale = 18)
private BigDecimal open;

// Similar for high, low, close

@NotNull(message = "Volume is required")
@DecimalMin(value = "0.0", message = "Volume cannot be negative")
private BigDecimal volume;

@NotBlank(message = "Timeframe is required")
@Pattern(regexp = "^(M[1-9]|M[1-5][0-9]|H[1-9]|H1[0-2]|D1|W1|MN1)$")
private String timeframe;
```

#### AiSignal.java
```java
@NotNull(message = "Direction is required")
private Direction direction;

@DecimalMin(value = "0.0", inclusive = false)
private BigDecimal entryPrice;

@AssertTrue(message = "LONG/SHORT signals must have entry and stopLoss")
private boolean isValidSignal() {
    if (direction == Direction.NEUTRAL) return true;
    return entryPrice != null && stopLoss != null;
}
```

**Impact:** 🔴 Critical - Better API error handling

---

### 1.3 Increase BigDecimal Precision
**Files:** All entities using BigDecimal

**Vấn đề:**
- `precision = 18, scale = 6` không đủ cho ultra-low cap coins
- SHIB: $0.000008 → OK
- Future coins: $0.0000000001 → FAIL

**Solution:**
```java
@Column(precision = 28, scale = 18)  // Support 10 decimal places + large numbers
private BigDecimal price;
```

**Impact:** 🟡 High - Future-proofing

---

## ⚠️ PHASE 2: ENTERPRISE FEATURES

### 2.1 Base Entity với Auditing
**New File:** `src/main/java/fpt/wongun/trading_ai/domain/entity/BaseEntity.java`

**Tạo base class:**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
    
    @CreatedBy
    @Column(length = 100, updatable = false)
    private String createdBy;
    
    @LastModifiedBy
    @Column(length = 100)
    private String lastModifiedBy;
    
    @Version
    private Long version;  // Optimistic locking
}
```

**Config:** `src/main/java/fpt/wongun/trading_ai/config/JpaAuditConfig.java`
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("SYSTEM"); // TODO: Get from SecurityContext
    }
}
```

**Update entities:**
- `Symbol extends BaseEntity`
- `Candle extends BaseEntity`
- `AiSignal extends BaseEntity` (remove duplicate createdAt/createdBy)

**Impact:** 🟡 High - Compliance & debugging

---

### 2.2 Soft Delete Implementation
**Update BaseEntity:**
```java
@MappedSuperclass
@SQLDelete(sql = "UPDATE {table} SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public abstract class BaseEntity {
    // ... existing fields
    
    @Column(nullable = false)
    private Boolean deleted = false;
    
    private Instant deletedAt;
    
    @Column(length = 100)
    private String deletedBy;
}
```

**Impact:** 🟡 High - Data recovery & compliance

---

### 2.3 Timeframe Enum (Type Safety)
**New File:** `src/main/java/fpt/wongun/trading_ai/domain/enums/Timeframe.java`

```java
@Getter
@AllArgsConstructor
public enum Timeframe {
    M1("1m", "1 Minute", 60),
    M5("5m", "5 Minutes", 300),
    M15("15m", "15 Minutes", 900),
    M30("30m", "30 Minutes", 1800),
    H1("1h", "1 Hour", 3600),
    H4("4h", "4 Hours", 14400),
    D1("1d", "1 Day", 86400),
    W1("1w", "1 Week", 604800);
    
    private final String binanceInterval;
    private final String displayName;
    private final int seconds;
    
    public static Timeframe fromBinanceInterval(String interval) {
        return Arrays.stream(values())
            .filter(tf -> tf.binanceInterval.equals(interval))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid interval: " + interval));
    }
}
```

**Update entities:**
```java
// Candle.java & AiSignal.java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 10)
private Timeframe timeframe;
```

**Update BinanceClient:**
```java
public static String mapTimeframeToInterval(Timeframe timeframe) {
    return timeframe.getBinanceInterval();
}
```

**Impact:** 🟢 Medium - Type safety (nhưng giảm flexibility)

---

### 2.4 Enhanced Direction Enum
**Update:** `src/main/java/fpt/wongun/trading_ai/domain/enums/Direction.java`

```java
@Getter
@AllArgsConstructor
public enum Direction {
    LONG("Buy", "Long", "↑", "#22c55e"),
    SHORT("Sell", "Short", "↓", "#ef4444"),
    NEUTRAL("Wait", "Neutral", "→", "#94a3b8");
    
    private final String action;
    private final String displayName;
    private final String arrow;
    private final String color;  // Hex color for frontend
}
```

**Impact:** 🟢 Low - Frontend consistency

---

### 2.5 Symbol Metadata Enhancement
**Update:** `src/main/java/fpt/wongun/trading_ai/domain/entity/Symbol.java`

```java
@Entity
public class Symbol extends BaseEntity {
    // ... existing fields
    
    @Column(precision = 8, scale = 2)
    private BigDecimal tickSize;  // Min price movement (0.01)
    
    @Column(precision = 8, scale = 6)
    private BigDecimal lotSize;   // Min order size (0.001 BTC)
    
    @Column(precision = 18, scale = 6)
    private BigDecimal minNotional;  // Min order value ($10)
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Embedded
    private TradingHours tradingHours;
}
```

**New Embeddable:** `src/main/java/fpt/wongun/trading_ai/domain/embeddable/TradingHours.java`
```java
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradingHours {
    
    @Column(length = 50)
    private String tradingDays;  // JSON: ["MONDAY", "TUESDAY", ...]
    
    private LocalTime marketOpenTime;
    
    private LocalTime marketCloseTime;
    
    @Column(length = 50)
    private String timezone;  // "America/New_York"
    
    public boolean isMarketOpen(Instant instant) {
        // Implementation to check if market is open
        // Consider timezone, trading days, hours
        return true; // TODO
    }
}
```

**Impact:** 🟢 Medium - Advanced trading logic

---

## 💡 PHASE 3: CODE QUALITY IMPROVEMENTS

### 3.1 Custom Exceptions
**New Package:** `src/main/java/fpt/wongun/trading_ai/exception/`

**Files to create:**
- `TradingException.java` (base)
- `InvalidSignalException.java`
- `SymbolNotFoundException.java`
- `InvalidTimeframeException.java`
- `MarketClosedException.java`

```java
@Getter
public class TradingException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public TradingException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = status;
    }
}
```

**Global Exception Handler:**
`src/main/java/fpt/wongun/trading_ai/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(TradingException.class)
    public ResponseEntity<ErrorResponse> handleTradingException(TradingException e) {
        log.error("Trading error: {}", e.getMessage());
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", errors));
    }
}
```

**Impact:** 🟢 Medium - Better error handling

---

### 3.2 Service Layer Refactoring
**Current issue:** Business logic scattered

**Create dedicated services:**
- `SymbolService.java`
- `CandleService.java`
- `SignalValidationService.java`
- `MarketHoursService.java`

**Impact:** 🟢 Medium - Code organization

---

### 3.3 DTOs với Validation
**Update all DTOs to include validation:**

```java
@Data
public class AiSuggestRequestDto {
    
    @NotBlank(message = "Symbol code is required")
    private String symbolCode;
    
    @NotNull(message = "Timeframe is required")
    private Timeframe timeframe;
    
    @NotNull(message = "Mode is required")
    private TradingMode mode;
    
    @Min(value = 20, message = "Minimum 20 candles required")
    @Max(value = 500, message = "Maximum 500 candles allowed")
    private Integer candleCount = 50;
}
```

**Impact:** 🟢 Low - API contract enforcement

---

## 🧪 PHASE 4: TESTING & QUALITY

### 4.1 Unit Tests
**Create test classes for:**
- `SymbolTest.java`
- `CandleTest.java`
- `AiSignalTest.java`
- `DirectionTest.java`
- `TimeframeTest.java`

**Impact:** 🔴 Critical - Quality assurance

---

### 4.2 Integration Tests
**Files:**
- `CandleRepositoryTest.java`
- `AiSignalServiceTest.java`
- `BinanceSyncSchedulerTest.java`

**Impact:** 🔴 Critical - Quality assurance

---

### 4.3 API Tests
**Files:**
- `AiSignalControllerTest.java`
- `CandleAdminControllerTest.java`

**Impact:** 🟡 High - API contract testing

---

## 📊 PHASE 5: OBSERVABILITY & MONITORING

### 5.1 Logging Enhancement
**Add structured logging:**
```java
@Slf4j
public class AiSignalService {
    
    public AiSignalResponseDto suggestTrade(...) {
        log.info("Trading signal request - symbol={}, timeframe={}, mode={}", 
                 symbol, timeframe, mode);
        
        MDC.put("symbol", symbol);
        MDC.put("timeframe", timeframe.name());
        
        try {
            // ... logic
            log.info("Signal generated - direction={}, entry={}, rr={}", 
                     direction, entry, rr);
            return response;
        } catch (Exception e) {
            log.error("Signal generation failed", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

**Impact:** 🟢 Medium - Debugging & monitoring

---

### 5.2 Metrics với Micrometer
**Add to pom.xml:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Custom metrics:**
```java
@Component
public class TradingMetrics {
    private final Counter signalsGenerated;
    private final Timer signalGenerationTime;
    
    public TradingMetrics(MeterRegistry registry) {
        signalsGenerated = Counter.builder("trading.signals.generated")
            .tag("type", "ai")
            .register(registry);
            
        signalGenerationTime = Timer.builder("trading.signal.generation.time")
            .register(registry);
    }
}
```

**Impact:** 🟢 Medium - Production monitoring

---

## 🔒 PHASE 6: SECURITY

### 6.1 API Key Authentication
**Add Spring Security:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Config:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**Impact:** 🔴 Critical (for production) - Security

---

### 6.2 Rate Limiting
**Add Bucket4j:**
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
</dependency>
```

**Implementation:**
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(...) {
        String apiKey = extractApiKey(request);
        Bucket bucket = resolveBucket(apiKey);
        
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("Too many requests");
        }
    }
}
```

**Impact:** 🔴 Critical - Prevent abuse

---

## 📦 PHASE 7: DEPLOYMENT

### 7.1 Docker Support
**Create:** `Dockerfile`
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Create:** `docker-compose.yml`
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:18-alpine
    environment:
      POSTGRES_DB: trading_ai
      POSTGRES_USER: trading_ai_user
      POSTGRES_PASSWORD: changeme
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      
  trading-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/trading_ai
      DB_USERNAME: trading_ai_user
      DB_PASSWORD: changeme
    depends_on:
      - postgres

volumes:
  postgres_data:
```

**Impact:** 🔴 Critical - Deployment

---

### 7.2 CI/CD Pipeline
**Create:** `.github/workflows/ci.yml`
```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build with Maven
        run: ./mvnw clean verify
      - name: Run tests
        run: ./mvnw test
      - name: Build Docker image
        run: docker build -t trading-ai:${{ github.sha }} .
```

**Impact:** 🟡 High - Automation

---

## 📈 SUMMARY: IMPLEMENTATION CHECKLIST

### 🔴 CRITICAL (Must do before production)
- [ ] 1.1 Unique constraint cho Candle
- [ ] 1.2 Validation annotations
- [ ] 1.3 BigDecimal precision increase
- [ ] 4.1 Unit tests
- [ ] 4.2 Integration tests
- [ ] 6.1 Authentication
- [ ] 6.2 Rate limiting
- [ ] 7.1 Docker support

### 🟡 HIGH (Should do for enterprise-grade)
- [ ] 2.1 Base entity với auditing
- [ ] 2.2 Soft delete
- [ ] 3.1 Custom exceptions
- [ ] 3.2 Service layer refactoring
- [ ] 4.3 API tests
- [ ] 5.1 Logging enhancement
- [ ] 7.2 CI/CD pipeline

### 🟢 MEDIUM (Nice to have)
- [ ] 2.3 Timeframe enum
- [ ] 2.4 Enhanced Direction enum
- [ ] 2.5 Symbol metadata
- [ ] 3.3 DTO validation
- [ ] 5.2 Metrics

---

## 🎯 ESTIMATED EFFORT

| Phase | Time | Complexity |
|-------|------|------------|
| Phase 1 | 4 hours | Low |
| Phase 2 | 8 hours | Medium |
| Phase 3 | 6 hours | Medium |
| Phase 4 | 12 hours | High |
| Phase 5 | 4 hours | Low |
| Phase 6 | 8 hours | High |
| Phase 7 | 4 hours | Medium |

**Total: ~46 hours (~1 week full-time)**

---

## 📝 MIGRATION NOTES

### Database Migration Strategy
1. Create backup: `pg_dump trading_ai > backup.sql`
2. Run Liquibase/Flyway migrations for schema changes
3. Test on staging environment
4. Blue-green deployment for production

### Breaking Changes
- ⚠️ Timeframe: String → Enum (API contract change)
- ⚠️ Soft delete: Affects all DELETE operations
- ⚠️ Authentication: All endpoints require auth

### Backward Compatibility
- Keep String timeframe support for 1 release
- Deprecation warnings in API responses
- Migration guide in README

---

**Document version:** 1.0  
**Last updated:** December 1, 2025  
**Author:** GitHub Copilot  
**Status:** 📋 Planning Phase
