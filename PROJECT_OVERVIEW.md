# Volman AI Trade Assistant - Tài Liệu Dự Án

## 📋 Tổng Quan

**Hệ thống**: AI phân tích giá theo phương pháp Bob Volman  
**Tech Stack**: Spring Boot 3 + React 19 + Groq AI + Binance API  
**Chức năng chính**: Phân tích nến, đưa ra tín hiệu LONG/SHORT/NEUTRAL với SL/TP

---

## 🏗️ Kiến Trúc Hệ Thống

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│  React Frontend │ ←─HTTP─→│ Spring Boot API  │ ←─API──→│  Groq AI (LLM)  │
│  (port 5173)    │         │  (port 8080)     │         │ Llama 3.3 70B   │
└─────────────────┘         └──────────────────┘         └─────────────────┘
                                      ↓
                            ┌──────────────────┐         ┌─────────────────┐
                            │  PostgreSQL DB   │ ←─API──→│  Binance API    │
                            │  (port 5432)     │         │ Real-time data  │
                            └──────────────────┘         └─────────────────┘
```

---

## 📁 Cấu Trúc Backend (trading-ai)

### Core Files

```
trading-ai/
├── src/main/java/fpt/wongun/trading_ai/
│   ├── TradingAiApplication.java           # Main + @EnableScheduling
│   │
│   ├── domain/entity/                      # Domain Models
│   │   ├── Candle.java                     # Nến: OHLCV + timestamp
│   │   ├── Symbol.java                     # Mã CP: BTCUSDT, XAUUSD
│   │   └── TradeSuggestion.java            # Tín hiệu AI lưu DB
│   │
│   ├── dto/                                # Data Transfer Objects
│   │   ├── AiSuggestRequestDto.java        # Request: symbol, timeframe, mode
│   │   └── AiSignalResponseDto.java        # Response: direction, SL, TP, reasoning
│   │
│   ├── controller/                         # REST Controllers
│   │   ├── AiSignalController.java         # POST /api/signals/ai-suggest
│   │   └── CandleAdminController.java      # POST /api/admin/candles/import-binance
│   │
│   ├── service/
│   │   ├── ai/
│   │   │   ├── GroqAiClient.java          # @Primary - Groq API client
│   │   │   ├── OpenAiClient.java          # Fallback khi groq.enabled=false
│   │   │   └── MockAiClient.java          # Testing
│   │   │
│   │   ├── market/
│   │   │   ├── BinanceClient.java         # Fetch candles từ Binance
│   │   │   ├── BinanceKline.java          # DTO cho Binance response
│   │   │   └── BinanceSyncScheduler.java  # Auto-sync mỗi 5 phút
│   │   │
│   │   └── AiSignalService.java           # Business logic chính
│   │
│   ├── repository/                         # JPA Repositories
│   │   ├── CandleRepository.java          # Query nến theo symbol/timeframe
│   │   ├── SymbolRepository.java          # Quản lý symbols
│   │   └── TradeSuggestionRepository.java # Lưu history signals
│   │
│   └── config/
│       ├── WebConfig.java                 # CORS config
│       ├── GroqProperties.java            # Groq API config
│       ├── OpenAiConfig.java              # OpenAI config
│       └── FakeCandleDataInitializer.java # Seed 200 nến XAUUSD khi start
│
└── src/main/resources/
    ├── application.yml                     # Config chính
    ├── application.properties              # DB connection
    ├── data.sql                            # SQL seed data
    └── .env                                # Secrets (gitignored)
```

### Key Classes Logic

#### 1. **AiSignalController**
```java
POST /api/signals/ai-suggest
{
  "symbolCode": "BTCUSDT",
  "timeframe": "M5",
  "mode": "SCALPING",
  "candleCount": 50
}
→ Fetch 50 nến mới nhất từ DB
→ Gọi GroqAiClient.suggestTrade()
→ Trả về: direction, entry, SL, TP1/2/3, reasoning
```

#### 2. **GroqAiClient**
```java
// SYSTEM_PROMPT: Bob Volman methodology
// - Trend: HH/HL (uptrend), LH/LL (downtrend)
// - Patterns: RBR, DBD, CPB, CPD
// - Overlap candles = indecision
// - Chỉ trade khi trend rõ ràng

suggestTrade() {
  1. Build candle summary (Open, High, Low, Close)
  2. Call Groq API với model: llama-3.3-70b-versatile
  3. Parse JSON response
  4. Apply Volman guards (reject unclear trends)
  5. Return TradeSuggestionDto
}
```

#### 3. **BinanceClient**
```java
fetchKlines(symbol, interval, limit) {
  → GET https://api.binance.com/api/v3/klines
  → Parse JSON array: [timestamp, open, high, low, close, volume, ...]
  → Return List<BinanceKline>
}

Mapping timeframes:
M5 → 5m, M15 → 15m, H1 → 1h, H4 → 4h, D1 → 1d
```

#### 4. **BinanceSyncScheduler**
```java
@Scheduled(fixedRate = 300000)  // 5 phút
syncLatestCandles() {
  1. Tìm tất cả CRYPTO symbols
  2. Mỗi symbol:
     - Fetch 20 nến mới nhất từ Binance
     - Xóa nến cũ trong DB
     - Lưu nến mới
  3. Log: "Synced X candles for BTCUSDT/M5"
}
```

### Database Schema

```sql
-- symbols table
id | code      | description  | type
1  | BTCUSDT   | Bitcoin      | CRYPTO
2  | ETHUSDT   | Ethereum     | CRYPTO
3  | XAUUSD    | Gold         | FOREX

-- candles table
id | symbol_id | timeframe | timestamp           | open    | high    | low     | close   | volume
1  | 1         | M5        | 2025-11-30 14:00:00 | 96500.0 | 96800.0 | 96400.0 | 96700.0 | 123.45

-- trade_suggestions table
id | symbol_id | timeframe | direction | entry_price | stop_loss | take_profit_1 | reasoning                  | created_at
1  | 1         | M5        | NEUTRAL   | null        | null      | null          | Trend unclear, mixed HH/LL | 2025-11-30 14:34:49
```

---

## 📁 Cấu Trúc Frontend (volman-ai-frontend)

```
volman-ai-frontend/
├── src/
│   ├── App.tsx                 # Main component (360 dòng)
│   ├── main.tsx                # Entry point
│   ├── index.css               # Tailwind CSS
│   └── assets/                 # Static files
│
├── public/                     # Public assets
├── index.html                  # HTML template
├── package.json                # Dependencies
├── vite.config.ts              # Vite config
├── tailwind.config.js          # Tailwind config
└── .env                        # VITE_API_BASE_URL=http://localhost:8080
```

### App.tsx Logic

```tsx
// State management
const [symbolCode, setSymbolCode] = useState('BTCUSDT')
const [timeframe, setTimeframe] = useState('M5')
const [mode, setMode] = useState('SCALPING')
const [latestSignal, setLatestSignal] = useState<AiSignalResponseDto | null>(null)
const [history, setHistory] = useState<SpringPage<AiSignalResponseDto> | null>(null)

// Load history on mount
useEffect(() => {
  fetch('/api/signals?symbolCode=BTCUSDT&timeframe=M5')
    .then(data => setHistory(data))
}, [symbolCode, timeframe])

// Generate AI signal
handleSubmit() {
  POST /api/signals/ai-suggest
  body: { symbolCode, timeframe, mode, maxRiskPerTrade }
  → setLatestSignal(response)
  → loadHistory() // Refresh table
}
```

### UI Components

```
┌─────────────────────────────────────────────────────────────┐
│ Header: Volman AI Trade Assistant                          │
│ Tags: Spring Boot 3 · Online | SCALPING                    │
└─────────────────────────────────────────────────────────────┘

┌────────────────────────┬────────────────────────────────────┐
│ Request Form           │ Latest Signal                      │
│                        │                                    │
│ Symbol: [BTCUSDT ▼]    │ Direction: [NEUTRAL]               │
│ ✓ Real-time Binance    │ BTCUSDT · M5                       │
│                        │ 2025-11-30 14:34:49               │
│ Timeframe: [M5 ▼]      │                                    │
│ Mode: [SCALPING ▼]     │ Entry: —                          │
│ Max Risk: [100]        │ Stop Loss: —                      │
│                        │ TP1/TP2/TP3: — / — / —            │
│ [Generate AI Signal]   │                                    │
│                        │ Reasoning: Trend unclear with      │
│                        │ mixed HH/LL sequences...           │
└────────────────────────┴────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Signal History (20 records)                                 │
│                                                             │
│ Symbol | TF | Direction | Entry | SL | TP1 | Created       │
│ BTCUSDT| M5 | NEUTRAL   | —     | —  | —   | 14:34:49      │
│ XAUUSD | M5 | LONG      | 2650  | 45 | 55  | 12:15:30      │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Environment Variables

### Backend (.env)
```bash
GROQ_ENABLED=true
GROQ_API_KEY=your_groq_api_key_here
GROQ_API_URL=https://api.groq.com/openai/v1/chat/completions
GROQ_MODEL=llama-3.3-70b-versatile

OPENAI_API_KEY=your_openai_key_here
OPENAI_MODEL=gpt-4o-mini

DB_URL=jdbc:postgresql://localhost:5432/trading_ai
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Frontend (.env)
```bash
VITE_API_BASE_URL=http://localhost:8080
```

---

## 🚀 API Endpoints

### Public APIs
```bash
# Generate AI signal
POST /api/signals/ai-suggest
Body: { symbolCode, timeframe, mode, candleCount?, maxRiskPerTrade? }
Response: { direction, entryPrice, stopLoss, takeProfit1/2/3, reasoning, ... }

# Get signal history
GET /api/signals?symbolCode=BTCUSDT&timeframe=M5&page=0&size=20
Response: { content: [...], totalElements, totalPages, ... }
```

### Admin APIs
```bash
# Import Binance data
POST /api/admin/candles/import-binance?symbol=BTCUSDT&timeframe=M5&limit=200
Response: { importedCount: 200, source: "Binance API", ... }

# Bulk import candles
POST /api/admin/candles/bulk-import
Body: [{ symbol, timeframe, timestamp, open, high, low, close, volume }, ...]

# Delete candles
DELETE /api/admin/candles?symbolCode=BTCUSDT&timeframe=M5
```

---

## 🔄 Data Flow (End-to-End)

### Flow 1: Generate AI Signal
```
User → Frontend → Backend → Database → AI → Response
  1. User nhập symbol=BTCUSDT, timeframe=M5, mode=SCALPING
  2. Frontend POST /api/signals/ai-suggest
  3. AiSignalController nhận request
  4. AiSignalService.generateSuggestion()
     - Fetch 50 nến mới nhất từ CandleRepository
     - Build candle summary text
  5. GroqAiClient.suggestTrade()
     - POST https://api.groq.com/openai/v1/chat/completions
     - Model: llama-3.3-70b-versatile
     - Prompt: SYSTEM (Volman rules) + USER (candle data)
  6. Parse AI response → TradeSuggestionDto
  7. Save to trade_suggestions table
  8. Return JSON to frontend
  9. Frontend hiển thị signal + reasoning
```

### Flow 2: Auto-Sync Binance Data
```
Scheduler → Binance API → Database
  1. Mỗi 5 phút: BinanceSyncScheduler.syncLatestCandles()
  2. Tìm tất cả CRYPTO symbols (BTCUSDT, ETHUSDT, ...)
  3. Mỗi symbol:
     - BinanceClient.fetchKlines(symbol, "5m", 20)
     - GET https://api.binance.com/api/v3/klines
     - Parse JSON array → List<BinanceKline>
  4. Convert BinanceKline → Candle entity
  5. Delete old candles: deleteBySymbolAndTimeframe()
  6. Save new candles: candleRepository.saveAll()
  7. Log: "Synced 20 candles for BTCUSDT/M5"
```

### Flow 3: Manual Binance Import
```
Admin → API → Binance → Database
  1. POST /api/admin/candles/import-binance?symbol=ETHUSDT&timeframe=M5&limit=200
  2. CandleAdminController.importFromBinance()
  3. BinanceClient.fetchKlines("ETHUSDT", "5m", 200)
  4. Create/get Symbol entity (auto-detect type=CRYPTO)
  5. Convert 200 BinanceKline → 200 Candle entities
  6. Delete old candles
  7. Save all 200 candles
  8. Response: { importedCount: 200, source: "Binance API" }
```

---

## 🧠 AI Prompt Engineering

### System Prompt (Bob Volman Rules)
```
You are a professional FX scalper following Bob Volman's price action methodology.

Rules:
1. Trend Analysis:
   - Uptrend: Higher Highs (HH) + Higher Lows (HL)
   - Downtrend: Lower Highs (LH) + Lower Lows (LL)
   - No trend: Mixed HH/LL or ranging

2. Entry Patterns:
   - RBR (Rally-Base-Rally): Buy breakout after consolidation in uptrend
   - DBD (Drop-Base-Drop): Sell breakout after consolidation in downtrend
   - CPB (Continuation Pattern Bullish): Small pullback in uptrend
   - CPD (Continuation Pattern Bearish): Small rally in downtrend

3. Overlap Analysis:
   - Many overlapping candles = indecision/consolidation
   - Clean separation = strong trend

4. Decision Logic:
   IF trend is clear AND setup is valid:
     RETURN LONG/SHORT with SL/TP
   ELSE:
     RETURN NEUTRAL with reasoning

Output JSON only: { direction, entryPrice, stopLoss, takeProfit1/2/3, reasoning }
```

### User Prompt Example
```
Analyze these 50 M5 candles for BTCUSDT in SCALPING mode:

Candle 1: O=96500 H=96550 L=96480 C=96520
Candle 2: O=96520 H=96600 L=96510 C=96580
...
Candle 50: O=96700 H=96750 L=96680 C=96720

Provide trade suggestion.
```

### AI Response
```json
{
  "direction": "NEUTRAL",
  "entryPrice": null,
  "stopLoss": null,
  "takeProfit1": null,
  "takeProfit2": null,
  "takeProfit3": null,
  "reasoning": "The trend is unclear with mixed HH/LL sequences, and the market shows overlapping and indecision candles, making it unsafe to enter a trade."
}
```

---

## 🛠️ Tech Stack Chi Tiết

### Backend
- **Spring Boot**: 3.4.12
- **Java**: 17
- **Database**: PostgreSQL 18.1
- **ORM**: Hibernate 6.6.36 (JPA)
- **HTTP Client**: WebClient (reactive)
- **Scheduler**: Spring @Scheduled
- **Build Tool**: Maven 3.9.x

### Frontend
- **React**: 19.2.0
- **TypeScript**: 5.x
- **Build Tool**: Vite 7.2.4
- **Styling**: Tailwind CSS 4.1.17
- **HTTP Client**: Fetch API

### External APIs
- **AI**: Groq API (Llama 3.3 70B, free)
- **Market Data**: Binance API (free, no key)
- **Fallback AI**: OpenAI GPT-4o-mini

---

## ⚙️ Cách Chạy Dự Án

### 1. Setup Database
```bash
# Start PostgreSQL
brew services start postgresql@14

# Create database
psql postgres
CREATE DATABASE trading_ai;
\q
```

### 2. Backend
```bash
cd trading-ai

# Set environment
export GROQ_API_KEY="your_groq_api_key_here"

# Run
./mvnw spring-boot:run

# Hoặc build JAR
./mvnw clean package
java -jar target/trading-ai-0.0.1-SNAPSHOT.jar
```

Backend chạy tại: http://localhost:8080

### 3. Frontend
```bash
cd volman-ai-frontend

# Install dependencies
npm install

# Run dev server
npm run dev
```

Frontend chạy tại: http://localhost:5173

### 4. Import Crypto Data
```bash
# Bitcoin
curl -X POST "http://localhost:8080/api/admin/candles/import-binance?symbol=BTCUSDT&timeframe=M5&limit=200"

# Ethereum
curl -X POST "http://localhost:8080/api/admin/candles/import-binance?symbol=ETHUSDT&timeframe=M5&limit=200"
```

### 5. Test AI Signal
```bash
curl -X POST "http://localhost:8080/api/signals/ai-suggest" \
  -H "Content-Type: application/json" \
  -d '{
    "symbolCode": "BTCUSDT",
    "timeframe": "M5",
    "mode": "SCALPING",
    "candleCount": 50
  }'
```

---

## 📊 Supported Symbols & Timeframes

### Cryptocurrencies (Real-time Binance)
- BTCUSDT (Bitcoin)
- ETHUSDT (Ethereum)
- BNBUSDT (Binance Coin)
- SOLUSDT (Solana)
- XRPUSDT (Ripple)

### Forex/Commodities (Mock Data)
- XAUUSD (Gold)
- EURUSD (Euro)
- GBPUSD (Pound)

### Timeframes
- M5 (5 minutes)
- M15 (15 minutes)
- M30 (30 minutes)
- H1 (1 hour)
- H4 (4 hours)
- D1 (1 day)

---

## 🔍 Testing & Validation

### Unit Tests
```bash
cd trading-ai
./mvnw test
```

### Integration Tests
```bash
# Test Binance import
curl -X POST "http://localhost:8080/api/admin/candles/import-binance?symbol=BTCUSDT&timeframe=M5&limit=100"
# Expected: { importedCount: 100 }

# Test AI signal
curl -X POST "http://localhost:8080/api/signals/ai-suggest" \
  -H "Content-Type: application/json" \
  -d '{"symbolCode":"BTCUSDT","timeframe":"M5","mode":"SCALPING","candleCount":50}'
# Expected: { direction: "LONG/SHORT/NEUTRAL", reasoning: "..." }

# Test history
curl "http://localhost:8080/api/signals?symbolCode=BTCUSDT&timeframe=M5&page=0&size=20"
# Expected: { content: [...], totalElements: X }
```

---

## 🚨 Common Issues & Solutions

### Issue 1: 401 Unauthorized from Groq
**Cause**: GROQ_API_KEY not loaded  
**Fix**: Export key before running
```bash
export GROQ_API_KEY="gsk_N3I8..."
./mvnw spring-boot:run
```

### Issue 2: CORS Error
**Cause**: Frontend origin not in allowed list  
**Fix**: Update application.yml
```yaml
cors:
  allowed-origins: http://localhost:5173
```

### Issue 3: No data from Binance
**Cause**: Invalid symbol or network issue  
**Fix**: Check symbol format (must be XXXUSDT) and internet connection

### Issue 4: Database connection refused
**Cause**: PostgreSQL not running  
**Fix**: 
```bash
brew services start postgresql@14
```

---

## 📈 Performance Metrics

- **AI Response Time**: 1-3 seconds (Groq)
- **Binance API Latency**: 200-500ms
- **Database Query**: 10-50ms (indexed)
- **Frontend Load**: <1 second
- **Auto-sync Frequency**: Every 5 minutes
- **API Rate Limits**: 1200 req/min (Binance)

---

## 🔐 Security

- ✅ Environment variables (.env files)
- ✅ CORS configured
- ✅ .gitignore for secrets
- ✅ No hardcoded API keys
- ✅ Input validation on API endpoints
- ✅ SQL injection prevention (JPA)

---

## 📝 Future Enhancements

1. **WebSocket Real-time Updates** - Binance WebSocket streams
2. **More Crypto Pairs** - ADAUSDT, DOGEUSDT, MATICUSDT
3. **Advanced Indicators** - RSI, MACD, Bollinger Bands
4. **Backtesting Module** - Test strategies với historical data
5. **Multi-timeframe Analysis** - Analyze M5 + M15 + H1 simultaneously
6. **User Authentication** - JWT tokens, role-based access
7. **Trade Execution** - Auto-place orders via Binance API
8. **Notification System** - Email/SMS alerts cho signals

---

## 📞 Support

**Repository**: 
- Backend: https://github.com/wongun78/trading-ai
- Frontend: https://github.com/wongun78/volman-ai-frontend

**Documentation**:
- BINANCE_INTEGRATION.md - Chi tiết Binance API
- README.md - Quick start guide

---

**Last Updated**: November 30, 2025  
**Version**: 1.0.0  
**Status**: ✅ Production Ready
