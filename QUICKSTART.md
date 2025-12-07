# 🚀 Quick Setup Guide - Enterprise Trading AI

## ✅ Prerequisites

### 1. Install Java 21 (Required)

Your system needs Java 21 LTS for this upgraded version.

#### macOS (Homebrew):
```bash
# Install Java 21
brew install openjdk@21

# Symlink for system Java wrappers
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Add to PATH permanently
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@21' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version  # Should show "openjdk version 21.0.9"
```

#### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
java -version
```

#### Windows:
Download from: https://adoptium.net/temurin/releases/?version=21

---

## 🔧 Quick Start

### 1. Copy Environment File
```bash
cp .env.example .env
nano .env  # Edit with your values
```

### 2. Required Environment Variables

**Minimum required in `.env`:**
```bash
# Database
DB_PASSWORD=your_password

# JWT
JWT_SECRET=dGhpcy1pcy1hLXNlY3VyZS1qd3Qtc2VjcmV0LWtleS1mb3ItdHJhZGluZy1haS1wbGVhc2UtY2hhbmdlLWluLXByb2R1Y3Rpb24=

# AI (at least one)
GROQ_API_KEY=gsk_xxxxx
# OR
OPENAI_API_KEY=sk_xxxxx
```

### 3. Run Application

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run

# Or use script
./run.sh
```

### 4. Access Application

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health**: http://localhost:8080/actuator/health

---

## 🔐 Default Credentials

```
Username: admin
Password: admin123
```

⚠️ **Change password in production!**

---

## 🐳 Docker Option

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

---

## 📝 Test Authentication

### 1. Register User:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trader1",
    "email": "trader1@example.com",
    "fullName": "John Trader",
    "password": "password123"
  }'
```

### 2. Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 3. Use Token:
```bash
# Copy token from login response
TOKEN="your_jwt_token_here"

# Make authenticated request
curl -X POST http://localhost:8080/api/signals/ai-suggest \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbolCode": "BTCUSDT",
    "timeframe": "M5",
    "mode": "SCALPING"
  }'
```

---

## 🎯 Project Status

✅ **Spring Security + JWT** - Token-based authentication  
✅ **Swagger/OpenAPI** - Interactive API docs  
✅ **Docker** - Production-ready deployment  
✅ **Role-Based Access** - ADMIN/TRADER/VIEWER  
✅ **Binance Integration** - Real-time crypto data (5s sync)  
✅ **Market Data**: 400 candles (BTCUSDT + ETHUSDT)  
⏳ **AI Signals**: Ready to generate  
⏳ **Trading**: 0 active positions  

See **README.md** for full documentation.

---

## 🐛 Troubleshooting

### Java version error:
```bash
# Check Java version
java -version

# Should show 21.x.x
# If not, install Java 21 (see above)
```

### Port already in use:
```bash
# Change port in .env
SERVER_PORT=8081
```

### Database connection failed:
```bash
# Start PostgreSQL
# Or use Docker Compose (includes PostgreSQL)
docker-compose up -d
```

---

**Full documentation**: See README.md and ENTERPRISE_UPGRADE.md
