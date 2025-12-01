#!/bin/bash

# Trading AI Backend - Quick Start Script
# Usage: ./run.sh

# Load environment variables from .env if exists
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Check if GROQ_API_KEY is set
if [ -z "$GROQ_API_KEY" ]; then
    echo "⚠️  Warning: GROQ_API_KEY not set!"
    echo "Please set it in .env file or export it manually"
    exit 1
fi

echo "🚀 Starting Trading AI Backend..."
echo "📊 Groq API Key: ${GROQ_API_KEY:0:10}...${GROQ_API_KEY: -10}"

# Run Spring Boot application
./mvnw spring-boot:run
