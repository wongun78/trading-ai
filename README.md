# Volman AI Trading System - Core Backend Engine

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.1-blue.svg)](https://www.postgresql.org/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-7.x-green.svg)](https://spring.io/projects/spring-security)
[![JWT](https://img.shields.io/badge/JWT-Auth-blueviolet.svg)](https://jwt.io/)

An enterprise-grade, high-performance algorithmic trading backend built to execute **Bob Volman's Price Action** methodology. This system leverages the ultra-fast computational power of **Groq AI (Llama 3.3 70B)** with an **OpenAI (GPT-4o-mini)** fallback to provide professional market analysis and trade signal generation. 

Built with scalability and maintainability in mind, this backend showcases strict architectural patterns, robust security, and seamless asynchronous market data integration.

## ✨ Enterprise Capabilities

### 🧠 Dual-Engine AI Analysis
- **High-Speed Inference:** Primary integration with Groq AI for real-time, low-latency market analysis.
- **Resilient Fallback:** Automatic redirection to OpenAI GPT if the primary engine encounters rate limits or downtime.
- **Volman Guards:** A strict validation layer that prevents irrational trades. Automatically calculates optimal risk/reward ratios (1.0 to 4.0), validates Stop-Loss distances based on entry types, and dynamically trims context (50/100/200 candles) matching Scalping, Intraday, or Swing modes.

### 🏢 Architectural Excellence
- **Domain-Driven Design (DDD) Influences:** Clean separation of concerns across Services, Repositories, Controllers, and DTOs.
- **BaseEntity Pattern:** Centralized JPA auditing (`createdAt`, `createdBy`, `updatedAt`), automated soft-delete workflows (`@SQLDelete`), and optimistic locking capabilities.
- **Type-Safe Engineering:** Extensive use of strict enums (`TradingMode`, `Timeframe`, `Direction`) ensuring data consistency from the database layer to the API response.
- **Resilient Error Handling:** A centralized `@ControllerAdvice` Global Exception Handler wrapper that maps custom domain exceptions (`SymbolNotFoundException`, `AiServiceException`) into unified, predictable JSON HTTP responses.

### 🔐 Bank-Grade Security
- **Stateless Authentication:** Spring Security 7.x configured with JWT (JSON Web Tokens) for fast, session-less request verification.
- **Role-Based Access Control (RBAC):** Pre-configured authorization tiers (ADMIN, TRADER, VIEWER) tightly bound to method-level security (`@PreAuthorize`).
- **Data Isolation:** Hardened service-layer filtering guarantees that users can only retrieve, modify, or interact with their own trading positions and signals.

### ⚡ Real-Time Market Integration
- **Asynchronous Binance Client:** Built with Spring WebFlux (`WebClient`) for non-blocking HTTP calls to the Binance Public API.
- **Automated Market Sync:** Scheduled polling mechanism running every 5 seconds to ingest multi-timeframe OHLCV (Open, High, Low, Close, Volume) data automatically, bypassing API key requirements while fully respecting rate limits.

## 🛠️ Technology Stack

- **Core:** Java 21, Spring Boot 3.4.12
- **Data Persistence:** PostgreSQL 18.1, Spring Data JPA, Flyway (for deterministic database migrations)
- **Performance & Async:** Caffeine Cache, Spring WebFlux
- **Security:** Spring Security, jjwt (0.12.6), BCrypt Password Encoding
- **API Documentation:** Springdoc OpenAPI (Swagger UI 2.7.0)

## 🚀 Getting Started

### Prerequisites
- JDK 21+
- PostgreSQL 18.1+
- Maven 3.9+
- Docker & Docker Compose (Optional for isolated local deployment)

### Local Environment Setup

1. **Database Initialization:**
   Ensure PostgreSQL is running. Flyway will automatically execute migrations located in `db/migration/` upon startup.

2. **Configuration:**
   Update your credentials and AI keys in `src/main/resources/application-local.yml` or inject them via environment variables:
   ```env
   GROQ_API_KEY=your_groq_key
   OPENAI_API_KEY=your_openai_key
   SPRING_DATASOURCE_USERNAME=your_db_user
   SPRING_DATASOURCE_PASSWORD=your_db_pass
   ```

3. **Build & Run:**
   ```bash
   ./mvnw clean install -DskipTests
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```
   *For containerized execution:*
   ```bash
   docker-compose up -d --build
   ```

4. **API Exploration:**
   Navigate to the auto-generated OpenAPI documentation to interact with endpoints directly:
   `http://localhost:8080/swagger-ui.html`

## 📄 License
This project is proprietary and confidential. Licensed under MIT for demonstration purposes.
