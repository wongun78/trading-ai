#!/bin/bash

# Trading AI Backend - Development Start Script
# Usage: ./dev.sh

echo "🔧 Development Mode - Auto-reload enabled"
echo ""

# Load .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Loaded .env file"
else
    echo "⚠️  No .env file found. Copy .env.example to .env"
    exit 1
fi

# Verify API key
if [ -z "$GROQ_API_KEY" ]; then
    echo "❌ GROQ_API_KEY not set in .env"
    exit 1
fi

echo "🚀 Starting Trading AI Backend..."
echo "📊 API Key: ${GROQ_API_KEY:0:10}...${GROQ_API_KEY: -10}"
echo "🌐 Server: http://localhost:${SERVER_PORT:-8080}"
echo "📝 Profile: ${SPRING_PROFILES_ACTIVE:-local}"
echo ""

# Run with Spring Boot DevTools for auto-reload
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
