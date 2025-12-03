# Trading AI - Enterprise-Grade AI Trading System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.1-blue.svg)](https://www.postgresql.org/)
[![Enterprise Grade](https://img.shields.io/badge/Enterprise-Grade-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Enterprise-grade AI trading system dựa trên **Bob Volman Price Action**, sử dụng Groq AI (Llama 3.3 70B) + OpenAI GPT-4 fallback. Includes type-safe enums, soft delete, auditing, global exception handling, và Volman Guards validation.

## 🔒 Security Notice

**⚠️ IMPORTANT**: If you cloned this repo before Dec 3, 2025, please read [SECURITY_NOTICE.md](SECURITY_NOTICE.md) immediately!

## 📋 Mục Lục

- [Security Notice](#-security-notice)
- [Tính Năng Chính](#-tính-năng-chính)
- [Công Nghệ](#-công-nghệ)
- [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Cài Đặt](#-cài-đặt)
- [Cấu Hình](#-cấu-hình)
- [Sử Dụng](#-sử-dụng)
- [API Documentation](#-api-documentation)
- [Phương Pháp Bob Volman](#-phương-pháp-bob-volman)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Đóng Góp](#-đóng-góp)

## ✨ Tính Năng Chính

### 🏢 Enterprise-Grade Architecture

- **BaseEntity Pattern**: Auditing fields (createdAt/By, updatedAt/By), soft delete, optimistic locking
- **Type-safe Enums**: TradingMode (SCALPING/INTRADAY/SWING), Timeframe (M1-W1), Direction (LONG/SHORT/NEUTRAL)
- **Custom Exception Hierarchy**: TradingException → SymbolNotFoundException, InvalidSignalException, MarketDataException, AiServiceException
- **ApiResponse Wrapper**: Consistent API responses với success/data/error/timestamp
- **Global Exception Handler**: Centralized error handling cho tất cả REST endpoints
- **JPA Auditing**: Tự động track created/modified by user và timestamp
- **Soft Delete Support**: @SQLDelete và @SQLRestriction cho recovery
- **Validation Framework**: @NotNull, @Min, @Max, @Valid trên entities và DTOs

### 🤖 AI Trading Engine

- **Groq AI Primary**: Llama 3.3 70B Versatile (ultra-fast, cost-effective)
- **OpenAI Fallback**: GPT-4o-mini khi Groq unavailable
- **Bob Volman Methodology**: Áp dụng chiến lược scalping/intraday/swing của chuyên gia Bob Volman
- **Volman Guards Validation**: Backend validates SL distance (LONG: SL < entry, SHORT: SL > entry), R:R ratio (1.0-4.0)
- **Context Trimming**: Auto-select candle count - SCALPING: 50, INTRADAY: 100, SWING: 200
- **Computed Fields**: isActionable(), potentialProfitTp1(), riskAmount() tự động tính toán

### 📊 Market Analysis

- **EMA21/EMA25**: Chỉ báo động hỗ trợ/kháng cự
- **Trend Detection**: Nhận diện xu hướng HH/HL (uptrend) và LH/LL (downtrend)
- **Pullback Identification**: Phát hiện các điểm pullback chất lượng cao
- **Candle Pattern Recognition**: Đọc hiểu rejection wick, pin bar, engulfing pattern

### 🎯 Trading Modes (Type-safe Enum)

```java
public enum TradingMode {
    SCALPING(50),   // 50 candles
    INTRADAY(100),  // 100 candles
    SWING(200);     // 200 candles
    
    private final int candleCount;
}
```

#### SCALPING Mode
- Phân tích 50 nến gần nhất
- Stop-loss chặt chẽ (< 0.4% entry price)
- Target: TP1 ≈ 1.2R-1.8R
- Phù hợp giao dịch nhanh, thoát lệnh sớm

#### INTRADAY Mode
- Phân tích 100 nến gần nhất
- Stop-loss rộng hơn (< 1.0% entry price)
- Target: TP1 ≈ 1.5R, TP2 ≈ 2.5R-3.0R
- Phù hợp giữ lệnh lâu hơn

#### SWING Mode
- Phân tích 200 nến gần nhất
- Stop-loss rộng nhất (< 2.0% entry price)
- Target: TP1 ≈ 2.0R, TP2 ≈ 3.5R-4.0R
- Phù hợp giữ lệnh nhiều ngày

### 🛡️ Safety Features

- **Volman Guards**: 
  - Validate SL direction (LONG: SL < entry, SHORT: SL > entry)
  - Check R:R ratio (1.0 ≤ R:R ≤ 4.0)
  - Ensure TP1 exists trước khi actionable
  - Throw InvalidSignalException nếu vi phạm
- **NEUTRAL Fallback**: Trả về NEUTRAL khi thị trường không rõ ràng
- **Custom Exceptions**: SymbolNotFoundException, InvalidSignalException, MarketDataException, AiServiceException
- **Global Exception Handler**: Consistent error responses với ApiResponse wrapper
- **Validation Annotations**: @NotNull, @Min, @Max, @Valid trên tất cả entities/DTOs

### 💾 Data Management

- **Bulk Candle Import**: Import hàng loạt dữ liệu nến
- **Fake Data Seeding**: Tự động tạo 200 nến giả (random walk) cho testing
- **Database Persistence**: Lưu trữ signals và candle data trong PostgreSQL
- **Signal History**: Tra cứu lịch sử gợi ý theo symbol/timeframe/date

## 🛠️ Công Nghệ

### Backend Framework
- **Spring Boot 3.4.12**: Framework chính
- **Spring Data JPA**: ORM và database access
- **Spring WebFlux**: Reactive HTTP client cho OpenAI API
- **Hibernate 6.6.36**: ORM engine

### Database
- **PostgreSQL 18.1**: Primary database
- **HikariCP**: Connection pooling
- **Flyway/Liquibase Ready**: Migration support

### AI/ML
- **Groq AI**: Primary engine - Llama 3.3 70B Versatile (ultra-fast inference)
- **OpenAI GPT-4**: Fallback engine - gpt-4o-mini
- **Strategy Pattern**: AiClient interface với multiple implementations
- **Custom Prompting**: Bob Volman-style system prompts
- **Temperature Control**: 0.3 (balanced consistency)

### Enterprise Patterns
- **BaseEntity**: Abstract class với auditing fields
- **Repository Pattern**: Spring Data JPA repositories
- **Service Layer**: Business logic separation
- **DTO Pattern**: Request/Response DTOs với validation
- **Exception Hierarchy**: Custom exceptions extending TradingException
- **ApiResponse Wrapper**: Generic wrapper cho consistent API responses
- **Soft Delete**: @SQLDelete annotation cho logical deletes
- **Optimistic Locking**: @Version field cho concurrency control

### Build & Dev Tools
- **Maven 3.9+**: Build tool
- **Lombok**: Boilerplate reduction
- **Jackson**: JSON processing
- **SLF4J + Logback**: Logging

### Testing & Quality
- **JUnit 5**: Unit testing
- **Spring Boot Test**: Integration testing
- **MockMVC**: REST API testing

## 🏗️ Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Web UI     │  │  Mobile App  │  │   API Client │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│         └──────────────────┴──────────────────┘                   │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTP/REST
┌─────────────────────────────┴───────────────────────────────────┐
│                      Controller Layer                            │
│  ┌────────────────────┐  ┌──────────────────────────────┐      │
│  │ AiSignalController │  │  CandleAdminController       │      │
│  │  - /api/signals/*  │  │  - /api/admin/candles/*      │      │
│  └─────────┬──────────┘  └──────────────┬───────────────┘      │
└────────────┼────────────────────────────┼──────────────────────┘
             │                             │
┌────────────┼────────────────────────────┼──────────────────────┐
│            │       Service Layer        │                       │
│  ┌─────────▼─────────┐      ┌──────────▼────────┐             │
│  │  AiSignalService  │      │  CandleService     │             │
│  └─────────┬─────────┘      └──────────┬─────────┘             │
│            │                            │                        │
│  ┌─────────▼─────────────┐  ┌──────────▼───────────┐          │
│  │ MarketAnalysisService │  │ FakeCandleInit       │          │
│  │  - buildContext()     │  │  - seedData()        │          │
│  └─────────┬─────────────┘  └──────────────────────┘          │
│            │                                                     │
│  ┌─────────▼──────────────────────────────────┐               │
│  │         AI Client (Strategy Pattern)        │               │
│  │  ┌──────────────┐      ┌───────────────┐   │               │
│  │  │ OpenAiClient │      │ MockAiClient  │   │               │
│  │  │  [@Primary]  │      │  [Fallback]   │   │               │
│  │  └──────┬───────┘      └───────────────┘   │               │
│  └─────────┼────────────────────────────────────┘              │
└────────────┼───────────────────────────────────────────────────┘
             │
┌────────────▼───────────────────────────────────────────────────┐
│                    External Services                            │
│  ┌──────────────────────────────────────────────────┐          │
│  │           OpenAI API (GPT-4)                      │          │
│  │  POST /v1/chat/completions                        │          │
│  │  - System Prompt (Bob Volman Rules)               │          │
│  │  - User Prompt (Market Context JSON)              │          │
│  │  - Response: Trade Suggestion JSON                │          │
│  └──────────────────────────────────────────────────┘          │
└────────────────────────────────────────────────────────────────┘
             │
┌────────────▼───────────────────────────────────────────────────┐
│                   Persistence Layer                             │
│  ┌──────────────────┐  ┌──────────────────┐                   │
│  │ AiSignalRepo     │  │  CandleRepo      │                   │
│  │ SymbolRepo       │  │                  │                   │
│  └────────┬─────────┘  └────────┬─────────┘                   │
│           │                      │                              │
│  ┌────────▼──────────────────────▼─────────┐                  │
│  │         PostgreSQL Database              │                  │
│  │  Tables: symbols, candles, ai_signals    │                  │
│  └──────────────────────────────────────────┘                  │
└────────────────────────────────────────────────────────────────┘
```

### Database Schema

```sql
-- Symbols table (with BaseEntity fields)
CREATE TABLE symbols (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255),
    type VARCHAR(50),
    
    -- BaseEntity auditing fields
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    
    -- Soft delete fields
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Candles table (enhanced precision + BaseEntity)
CREATE TABLE candles (
    id BIGSERIAL PRIMARY KEY,
    symbol_id BIGINT REFERENCES symbols(id),
    timeframe VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    open DECIMAL(28, 18),      -- Increased from 18,6 for low-cap coins
    high DECIMAL(28, 18),
    low DECIMAL(28, 18),
    close DECIMAL(28, 18),
    volume DECIMAL(28, 18),
    
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    CONSTRAINT unique_candle UNIQUE(symbol_id, timeframe, timestamp)
);

-- AI Signals table (with mode + BaseEntity + validation)
CREATE TABLE ai_signals (
    id BIGSERIAL PRIMARY KEY,
    symbol_id BIGINT REFERENCES symbols(id),
    timeframe VARCHAR(10) NOT NULL,
    mode VARCHAR(20) NOT NULL,     -- SCALPING, INTRADAY, SWING
    direction VARCHAR(10) NOT NULL, -- LONG, SHORT, NEUTRAL
    entry_price DECIMAL(28, 18),
    stop_loss DECIMAL(28, 18),
    take_profit1 DECIMAL(28, 18),
    take_profit2 DECIMAL(28, 18),
    take_profit3 DECIMAL(28, 18),
    risk_reward1 DECIMAL(10, 2),
    risk_reward2 DECIMAL(10, 2),
    risk_reward3 DECIMAL(10, 2),
    reasoning TEXT,
    
    -- BaseEntity auditing fields
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    
    -- Soft delete
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    -- @PrePersist validation ensures mode matches enum
    CONSTRAINT valid_mode CHECK (mode IN ('SCALPING', 'INTRADAY', 'SWING')),
    CONSTRAINT valid_direction CHECK (direction IN ('LONG', 'SHORT', 'NEUTRAL'))
);
```

## 📦 Yêu Cầu Hệ Thống

### Required
- **Java**: JDK 17 or higher
- **PostgreSQL**: 14.0 or higher (tested with 18.1)
- **Maven**: 3.9+ (or use included `./mvnw`)
- **OpenAI API Key**: For production AI features

### Optional
- **Docker**: For containerized deployment
- **Git**: For version control

### System Resources
- **RAM**: Minimum 2GB, recommended 4GB
- **Disk**: 500MB for application + database space
- **CPU**: 2+ cores recommended

## 🚀 Cài Đặt

### 1. Clone Repository

```bash
git clone https://github.com/wongun78/trading-ai.git
cd trading-ai
```

### 2. Setup PostgreSQL Database

```bash
# Tạo database và user
sudo -u postgres psql

CREATE DATABASE trading_ai;
CREATE USER trading_ai_user WITH PASSWORD '123456';
GRANT ALL PRIVILEGES ON DATABASE trading_ai TO trading_ai_user;
\q
```

### 3. Cấu Hình Application

Chỉnh sửa `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_ai
    username: trading_ai_user
    password: your_secure_password
    
openai:
  api-key: ${OPENAI_API_KEY:changeme}
  base-url: https://api.openai.com/v1
  model: gpt-4o-mini
  temperature: 0.3
```

### 4. Set OpenAI API Key

```bash
# Linux/Mac
export OPENAI_API_KEY=sk-your-actual-openai-key-here

# Windows (PowerShell)
$env:OPENAI_API_KEY="sk-your-actual-openai-key-here"
```

### 5. Build & Run

```bash
# Build project
./mvnw clean package -DskipTests

# Run application
java -jar target/trading-ai-0.0.1-SNAPSHOT.jar

# Hoặc chạy trực tiếp với Maven
./mvnw spring-boot:run
```

### 6. Verify Installation

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response
{"status":"UP"}
```

## ⚙️ Cấu Hình

### Application Properties

```yaml
# application.yml
spring:
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/trading_ai
    username: trading_ai_user
    password: ${DB_PASSWORD:123456}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  
  # JPA/Hibernate
  jpa:
    hibernate:
      ddl-auto: update  # update | validate | create | create-drop
    show-sql: true      # false for production
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Server Configuration
server:
  port: 8080
  compression:
    enabled: true

# Groq AI Configuration (Primary)
groq:
  api-key: ${GROQ_API_KEY}  # Required! No default
  base-url: https://api.groq.com/openai/v1
  model: llama-3.3-70b-versatile
  temperature: 0.3

# OpenAI Configuration (Fallback)
openai:
  api-key: ${OPENAI_API_KEY}  # Required! No default
  base-url: https://api.openai.com/v1
  model: gpt-4o-mini
  temperature: 0.3
  timeout:
    response: 60s
    connect: 10s

# Logging
logging:
  level:
    root: INFO
    fpt.wongun.trading_ai: DEBUG
    org.hibernate.SQL: DEBUG
```

### Environment Variables

```bash
# Required (AI Services)
GROQ_API_KEY=gsk-xxxxxxxxxxxxx        # Groq AI (Primary)
OPENAI_API_KEY=sk-xxxxxxxxxxxxx       # OpenAI (Fallback)

# Required (Database)
DB_PASSWORD=secure_password

# Optional
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=your-secret-key            # For future auth
REDIS_HOST=localhost                  # For future caching
REDIS_PORT=6379
```

## 📖 Sử Dụng

### 1. Automatic Fake Data Seeding

Hệ thống tự động tạo 200 nến XAUUSD/M5 khi khởi động lần đầu:

```
2025-11-30 INFO: Seeding 200 fake candles for XAUUSD/M5
2025-11-30 INFO: Successfully seeded 200 fake candles
```

### 2. Generate AI Trading Signal

```bash
# SCALPING mode
curl -X POST http://localhost:8080/api/signals/ai-suggest \
  -H "Content-Type: application/json" \
  -d '{
    "symbolCode": "XAUUSD",
    "timeframe": "M5",
    "mode": "SCALPING"
  }'

# INTRADAY mode
curl -X POST http://localhost:8080/api/signals/ai-suggest \
  -H "Content-Type: application/json" \
  -d '{
    "symbolCode": "XAUUSD",
    "timeframe": "M5",
    "mode": "INTRADAY"
  }'
```

**Response Example (LONG signal with ApiResponse wrapper):**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "symbolCode": "XAUUSD",
    "timeframe": "M5",
    "mode": "SCALPING",
    "direction": "LONG",
    "entryPrice": 2005.50,
    "stopLoss": 2003.20,
    "takeProfit1": 2008.95,
    "takeProfit2": 2012.40,
    "takeProfit3": null,
    "riskReward1": 1.5,
    "riskReward2": 3.0,
    "riskReward3": null,
    "reasoning": "Clean HH/HL uptrend. Price pulled back to EMA21 with strong rejection wick.",
    "createdAt": "2025-12-01T06:30:00Z",
    "createdBy": "SYSTEM",
    "lastModifiedAt": "2025-12-01T06:30:00Z",
    "lastModifiedBy": "SYSTEM",
    "version": 0,
    "actionable": true,
    "potentialProfitTp1": 3.45,
    "riskAmount": 2.30
  },
  "error": null,
  "timestamp": "2025-12-01T06:30:00Z"
}
```

**Response Example (NEUTRAL - No trade):**

```json
{
  "success": true,
  "data": {
    "id": null,
    "symbolCode": "XAUUSD",
    "timeframe": "M5",
    "mode": "SCALPING",
    "direction": "NEUTRAL",
    "entryPrice": null,
    "stopLoss": null,
    "takeProfit1": null,
    "takeProfit2": null,
    "takeProfit3": null,
    "riskReward1": null,
    "riskReward2": null,
    "riskReward3": null,
    "reasoning": "Mixed HH/LL structure. No clear EMA reaction. Market too choppy.",
    "createdAt": "2025-12-01T06:31:00Z",
    "actionable": false,
    "potentialProfitTp1": null,
    "riskAmount": null
  },
  "error": null,
  "timestamp": "2025-12-01T06:31:00Z"
}
```

**Error Response Example:**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "SYMBOL_NOT_FOUND",
    "message": "Symbol INVALID not found in database",
    "details": {
      "symbolCode": "INVALID",
      "availableSymbols": ["BTCUSDT", "ETHUSDT", "XAUUSD"]
    }
  },
  "timestamp": "2025-12-01T06:32:00Z"
}
```

### 3. Get Signal History

```bash
curl "http://localhost:8080/api/signals?symbolCode=XAUUSD&timeframe=M5&page=0&size=10"
```

### 4. Bulk Import Candles

```bash
curl -X POST http://localhost:8080/api/admin/candles/bulk-import \
  -H "Content-Type: application/json" \
  -d '[
    {
      "symbolCode": "EURUSD",
      "timeframe": "M5",
      "timestamp": "2025-11-30T06:00:00Z",
      "open": 1.0850,
      "high": 1.0865,
      "low": 1.0845,
      "close": 1.0860,
      "volume": 1500.0
    }
  ]'
```

### 5. Delete Candles

```bash
# Delete all candles for XAUUSD/M5
curl -X DELETE "http://localhost:8080/api/admin/candles?symbolCode=XAUUSD&timeframe=M5"

# Delete all candles for XAUUSD (all timeframes)
curl -X DELETE "http://localhost:8080/api/admin/candles?symbolCode=XAUUSD"
```

## 📚 API Documentation

### Endpoints

#### AI Signal Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/signals/ai-suggest` | Generate AI trade suggestion |
| GET | `/api/signals` | Get signal history (paginated) |

#### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/candles/bulk-import` | Import candles in bulk |
| DELETE | `/api/admin/candles` | Delete candles by symbol/timeframe |

#### Health Check

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Application health status |

### Request/Response Models

#### AiSuggestRequestDto

```json
{
  "symbolCode": "XAUUSD",     // Required: XAUUSD, EURUSD, etc.
  "timeframe": "M5",          // Required: M1, M5, M15, H1, H4, D1
  "mode": "SCALPING"          // Required: SCALPING | INTRADAY
}
```

#### CandleImportDto

```json
{
  "symbolCode": "XAUUSD",                    // Required
  "timeframe": "M5",                         // Required
  "timestamp": "2025-11-30T06:00:00Z",      // Required (ISO 8601)
  "open": 2005.50,                          // Required
  "high": 2008.20,                          // Required
  "low": 2004.80,                           // Required
  "close": 2007.10,                         // Required
  "volume": 1500.0                          // Required
}
```

## 🎓 Phương Pháp Bob Volman

### Core Principles

1. **Pure Price Action**
   - Chỉ giao dịch dựa trên candle patterns
   - Không sử dụng indicators phức tạp
   - EMA21/25 chỉ là dynamic support/resistance

2. **Market Structure**
   - **Uptrend**: Higher Highs (HH) + Higher Lows (HL)
   - **Downtrend**: Lower Highs (LH) + Lower Lows (LL)
   - **Sideways**: Mixed structure → tránh giao dịch

3. **Entry Requirements**
   - Pullback vào vùng EMA21/25
   - Rejection wick hoặc strong momentum candle
   - Trend phải rõ ràng và sạch
   - Không chase extended moves

4. **Risk Management**
   - Stop-loss chặt chẽ ngay dưới/trên swing point
   - Risk/Reward tối thiểu 1:1.5 (SCALPING) hoặc 1:2 (INTRADAY)
   - Thoát lệnh sớm nếu price action không confirm

### AI Implementation

Hệ thống áp dụng Bob Volman methodology thông qua:

```python
# System Prompt (simplified)
You are Bob Volman, expert price action trader.

Rules:
- Trade ONLY clean price action
- Use EMA21/25 as dynamic S/R
- Check HH/HL or LH/LL structure
- Pullback + rejection = entry
- Unclear setup = NEUTRAL

# Volman Guards (post-processing)
1. SL Distance Check
   - SCALPING: < 0.4% entry
   - INTRADAY: < 1.0% entry
   
2. R:R Validation
   - Must be between 1.0 - 4.0
   - Realistic targets only
   
3. Entry/SL Required
   - No trade without both values
```

## 🧪 Testing

### Run Unit Tests

```bash
./mvnw test
```

### Run Integration Tests

```bash
./mvnw verify
```

### Manual API Testing

```bash
# Test với fake data (không cần OpenAI key)
curl -X POST http://localhost:8080/api/signals/ai-suggest \
  -H "Content-Type: application/json" \
  -d '{"symbolCode": "XAUUSD", "timeframe": "M5", "mode": "SCALPING"}'

# Kết quả mong đợi: NEUTRAL (vì không có OpenAI key)
# {
#   "direction": "NEUTRAL",
#   "reasoning": "AI service unavailable: 401 Unauthorized..."
# }
```

### Test Coverage

```bash
# Generate coverage report
./mvnw clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

## 🚢 Deployment

### Docker Deployment

**Dockerfile:**

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build & Run:**

```bash
# Build Docker image
docker build -t trading-ai:latest .

# Run with Docker Compose
docker-compose up -d
```

**docker-compose.yml:**

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: trading_ai
      POSTGRES_USER: trading_ai_user
      POSTGRES_PASSWORD: secure_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/trading_ai
      SPRING_DATASOURCE_USERNAME: trading_ai_user
      SPRING_DATASOURCE_PASSWORD: secure_password
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      - postgres

volumes:
  postgres_data:
```

### Production Checklist

- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure secure database password
- [ ] Set production `GROQ_API_KEY` (primary)
- [ ] Set production `OPENAI_API_KEY` (fallback)
- [ ] Enable HTTPS/SSL
- [ ] Configure logging to file
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure backup strategy
- [ ] Review security headers
- [ ] Load testing completed
- [ ] Set up JPA auditing with real user context (replace "SYSTEM")
- [ ] Configure Redis for distributed caching
- [ ] Implement Spring Security + JWT authentication
- [ ] Set up database migration with Flyway/Liquibase
- [ ] Configure soft delete retention policy

## 🤝 Đóng Góp

Contributions are welcome! Please follow these steps:

### 1. Fork the Project

```bash
git clone https://github.com/wongun78/trading-ai.git
cd trading-ai
```

### 2. Create Feature Branch

```bash
git checkout -b feature/amazing-feature
```

### 3. Commit Changes

```bash
git commit -m "Add amazing feature"
```

### 4. Push to Branch

```bash
git push origin feature/amazing-feature
```

### 5. Open Pull Request

Submit PR với mô tả chi tiết về changes.

### Code Style Guidelines

- Follow Spring Boot best practices
- Use Lombok for boilerplate reduction
- Write meaningful commit messages
- Add JavaDoc for public methods
- Include unit tests for new features
- Keep methods focused and small

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔄 Changelog

### v2.0.0 - Enterprise-Grade Upgrade (2025-12-01)

**Major Changes:**
- ✅ **BaseEntity Pattern**: Auditing fields (createdAt/By, updatedAt/By), soft delete, optimistic locking
- ✅ **Type-safe Enums**: TradingMode (SCALPING/INTRADAY/SWING), Timeframe (M1-W1)
- ✅ **Custom Exception Hierarchy**: 5 exceptions extending TradingException
- ✅ **ApiResponse Wrapper**: Generic wrapper cho consistent API responses
- ✅ **Global Exception Handler**: Centralized error handling
- ✅ **Enhanced Direction Enum**: Display metadata (colors, arrows, actions)
- ✅ **Volman Guards**: Backend validation (SL direction, R:R ratio, TP1 existence)
- ✅ **Computed Fields**: isActionable(), potentialProfitTp1(), riskAmount()
- ✅ **JPA Auditing**: AuditorAware bean với "SYSTEM" default
- ✅ **Enhanced Precision**: DECIMAL(28,18) for low-cap coin support
- ✅ **Unique Constraints**: UNIQUE(symbol_id, timeframe, timestamp) on candles
- ✅ **Groq AI Primary**: Llama 3.3 70B với OpenAI fallback
- ✅ **Validation Annotations**: @NotNull, @Min, @Max across all layers
- ✅ **Binance Integration**: Live candle sync scheduler (every 5 minutes)

**Documentation:**
- ✅ ENTERPRISE_UPGRADE_PLAN.md - 12-phase upgrade roadmap
- ✅ UPGRADE_COMPLETED.md - Detailed completion report
- ✅ Updated README.md with enterprise features

**Metrics:**
- Overall Score: 67.5% → 97.5%
- Files Modified: 29 (13 new, 16 enhanced)
- Code Quality: Enterprise-grade

### v1.0.0 - Initial Release (2025-11-30)
- ✅ Spring Boot 3.4.12 + Java 17
- ✅ PostgreSQL 18.1 integration
- ✅ OpenAI GPT-4 integration
- ✅ Bob Volman methodology
- ✅ SCALPING + INTRADAY modes
- ✅ Fake data seeding
- ✅ Bulk candle import
- ✅ Signal history API

## 📚 Related Documentation

- [ENTERPRISE_UPGRADE_PLAN.md](./ENTERPRISE_UPGRADE_PLAN.md) - 12-phase upgrade roadmap
- [UPGRADE_COMPLETED.md](./UPGRADE_COMPLETED.md) - Enterprise upgrade completion report
- [BINANCE_INTEGRATION.md](./BINANCE_INTEGRATION.md) - Binance API integration guide
- [PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md) - Technical deep dive
- [Frontend README](../volman-ai-frontend/README.md) - React frontend documentation

## 📚 Learning Resources

### Bob Volman
- **Forex Price Action Scalping** - Core methodology book
- **Price Action Trends & Ranges** - Advanced techniques

### Spring Boot & Enterprise Patterns
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Guide](https://spring.io/guides/gs/accessing-data-jpa/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/)

### AI/ML Trading
- [Groq AI Documentation](https://console.groq.com/docs)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [Llama 3.3 Model Card](https://huggingface.co/meta-llama/Llama-3.3-70B-Instruct)

## 🔧 Troubleshooting

### Groq API Unauthorized (401)

```
Groq AI client failed: 401 Unauthorized
```

**Solution**: Kiểm tra GROQ_API_KEY environment variable:

```bash
export GROQ_API_KEY=gsk-your-actual-groq-key-here
```

### OpenAI Fallback Not Working

```
AI service unavailable: Both Groq and OpenAI failed
```

**Solution**: Kiểm tra cả hai API keys:

```bash
export GROQ_API_KEY=gsk-xxxxx
export OPENAI_API_KEY=sk-xxxxx
```

### Invalid Signal Exception

```
InvalidSignalException: LONG signal with SL > entry
```

**Solution**: Volman Guards validation failed. Kiểm tra:
- LONG signals: SL must be < entry price
- SHORT signals: SL must be > entry price
- R:R ratio: Must be between 1.0 - 4.0
- TP1: Must exist for actionable signals

### Soft Delete Recovery

```sql
-- View deleted signals
SELECT * FROM ai_signals WHERE deleted = true;

-- Restore deleted signal
UPDATE ai_signals 
SET deleted = false, deleted_at = NULL, deleted_by = NULL 
WHERE id = 123;
```

### Optimistic Locking Conflict

```
OptimisticLockException: Row was updated by another transaction
```

**Solution**: Retry operation hoặc reload entity:

```java
@Transactional
public AiSignal updateSignal(Long id, AiSignalDto dto) {
    AiSignal signal = repository.findById(id)
        .orElseThrow(() -> new SymbolNotFoundException("Signal not found"));
    // Update fields...
    return repository.save(signal); // @Version auto-increments
}
```

## 📞 Contact

**Project Owner**: wongun78  
**Repository**: [https://github.com/wongun78/trading-ai](https://github.com/wongun78/trading-ai)  
**Issues**: [https://github.com/wongun78/trading-ai/issues](https://github.com/wongun78/trading-ai/issues)

---

**⚠️ Disclaimer**: Educational project. Trading carries risk. Not financial advice. Never invest more than you can afford to lose.

**Made with ❤️ using Spring Boot 3 + Groq AI (Llama 3.3 70B) + PostgreSQL 18**
