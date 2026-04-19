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

# Run with Spring Boot DevTools for auto-reload
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
