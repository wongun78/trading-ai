#!/bin/bash

# Trading AI Backend - Production Build & Run
# Usage: ./start.sh

echo "🏭 Production Mode"
echo ""

# Load .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Loaded .env file"
fi

# Build
echo "🔨 Building application..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# Run
echo ""
echo "🚀 Starting application..."
java -jar target/trading-ai-0.0.1-SNAPSHOT.jar
