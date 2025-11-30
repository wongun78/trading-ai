# Trading AI - AI-Powered Price Action Trading System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.1-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Hệ thống AI gợi ý giao dịch tự động dựa trên phương pháp **Bob Volman Price Action**, sử dụng OpenAI GPT-4 để phân tích biểu đồ nến và đưa ra quyết định giao dịch chuyên nghiệp.

## 📋 Mục Lục

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

### 🤖 AI Trading Engine

- **OpenAI GPT-4 Integration**: Sử dụng mô hình ngôn ngữ tiên tiến để phân tích price action
- **Bob Volman Methodology**: Áp dụng chiến lược scalping/intraday của chuyên gia Bob Volman
- **Volman Guards**: Hệ thống kiểm tra đa lớp để đảm bảo gợi ý an toàn và thực tế
- **Context Trimming**: Tối ưu hóa phân tích với 50 nến (SCALPING) hoặc 100 nến (INTRADAY)

### 📊 Market Analysis

- **EMA21/EMA25**: Chỉ báo động hỗ trợ/kháng cự
- **Trend Detection**: Nhận diện xu hướng HH/HL (uptrend) và LH/LL (downtrend)
- **Pullback Identification**: Phát hiện các điểm pullback chất lượng cao
- **Candle Pattern Recognition**: Đọc hiểu rejection wick, pin bar, engulfing pattern

### 🎯 Trading Modes

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

### 🛡️ Safety Features

- **Volman Guards**: Kiểm tra SL distance và R:R ratio
- **NEUTRAL Fallback**: Trả về NEUTRAL khi thị trường không rõ ràng
- **Risk Management**: Giới hạn R:R trong khoảng 1.0-4.0
- **Error Handling**: Xử lý lỗi graceful khi OpenAI không khả dụng

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
- **OpenAI GPT-4**: Primary AI engine (gpt-4o-mini)
- **Custom Prompting**: Bob Volman-style system prompts
- **Temperature Control**: 0.3 (balanced consistency)

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
-- Symbols table
CREATE TABLE symbols (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255),
    type VARCHAR(50)
);

-- Candles table
CREATE TABLE candles (
    id BIGSERIAL PRIMARY KEY,
    symbol_id BIGINT REFERENCES symbols(id),
    timeframe VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    open DECIMAL(18, 6),
    high DECIMAL(18, 6),
    low DECIMAL(18, 6),
    close DECIMAL(18, 6),
    volume DECIMAL(18, 6),
    UNIQUE(symbol_id, timeframe, timestamp)
);

-- AI Signals table
CREATE TABLE ai_signals (
    id BIGSERIAL PRIMARY KEY,
    symbol_id BIGINT REFERENCES symbols(id),
    timeframe VARCHAR(10) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    entry_price DECIMAL(18, 6),
    stop_loss DECIMAL(18, 6),
    take_profit1 DECIMAL(18, 6),
    take_profit2 DECIMAL(18, 6),
    take_profit3 DECIMAL(18, 6),
    risk_reward1 DECIMAL(18, 6),
    risk_reward2 DECIMAL(18, 6),
    risk_reward3 DECIMAL(18, 6),
    reasoning TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
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

# OpenAI Configuration
openai:
  api-key: ${OPENAI_API_KEY:changeme}
  base-url: https://api.openai.com/v1
  model: gpt-4o-mini  # gpt-4 | gpt-4-turbo | gpt-4o-mini
  temperature: 0.3    # 0.0-1.0 (lower = more consistent)

# Logging
logging:
  level:
    root: INFO
    fpt.wongun.trading_ai: DEBUG
    org.hibernate.SQL: DEBUG
```

### Environment Variables

```bash
# Required
OPENAI_API_KEY=sk-xxxxxxxxxxxxx

# Optional
DB_PASSWORD=secure_password
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production
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

**Response Example (LONG signal):**

```json
{
  "id": 1,
  "symbolCode": "XAUUSD",
  "timeframe": "M5",
  "direction": "LONG",
  "entryPrice": 2005.50,
  "stopLoss": 2003.20,
  "takeProfit1": 2008.95,
  "takeProfit2": 2012.40,
  "takeProfit3": null,
  "riskReward1": 1.5,
  "riskReward2": 3.0,
  "riskReward3": null,
  "reasoning": "Clean HH/HL uptrend. Price pulled back to EMA21 with strong rejection wick. Entry at pullback completion.",
  "createdAt": "2025-11-30T06:30:00Z"
}
```

**Response Example (NEUTRAL - No trade):**

```json
{
  "id": null,
  "symbolCode": "XAUUSD",
  "timeframe": "M5",
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
  "createdAt": "2025-11-30T06:31:00Z"
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
- [ ] Set production `OPENAI_API_KEY`
- [ ] Enable HTTPS/SSL
- [ ] Configure logging to file
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure backup strategy
- [ ] Review security headers
- [ ] Load testing completed

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

## 🙏 Acknowledgments

- **Bob Volman** - Price action trading methodology
- **OpenAI** - GPT-4 API for AI analysis
- **Spring Boot Team** - Excellent framework
- **PostgreSQL Community** - Robust database

## 📞 Contact

**Project Owner**: wongun78  
**Repository**: [https://github.com/wongun78/trading-ai](https://github.com/wongun78/trading-ai)  
**Issues**: [https://github.com/wongun78/trading-ai/issues](https://github.com/wongun78/trading-ai/issues)

---

**⚠️ Disclaimer**: This is an educational project. Trading financial instruments carries risk. Always do your own research and never invest more than you can afford to lose. The AI suggestions are not financial advice.

**Made with ❤️ using Spring Boot & OpenAI**
