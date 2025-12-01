# Trading AI - Quick Start Guide

## 🚀 Chạy Backend (Cách Nhanh)

### Lần đầu tiên:
```bash
# 1. Copy .env.example sang .env (đã có sẵn rồi)
# 2. Chạy script
./run.sh
```

### Các lần sau:
```bash
# Development mode (auto-reload)
./dev.sh

# Hoặc production mode
./start.sh

# Hoặc quick run
./run.sh
```

---

## 📝 Chi Tiết Scripts

### `./run.sh` - Quick Start
```bash
./run.sh
```
- Load `.env` file tự động
- Check GROQ_API_KEY
- Chạy `mvnw spring-boot:run`
- **Dùng cho**: Development nhanh

### `./dev.sh` - Development Mode  
```bash
./dev.sh
```
- Load `.env` file
- Enable Spring DevTools (auto-reload)
- Show config info (API key, port, profile)
- **Dùng cho**: Development với auto-reload

### `./start.sh` - Production Mode
```bash
./start.sh
```
- Build JAR file (`mvn clean package`)
- Run JAR file
- **Dùng cho**: Production/Testing

---

## 🔑 Environment Variables

File `.env` đã có sẵn với:
```env
GROQ_API_KEY=gsk_your_actual_groq_api_key_here
OPENAI_API_KEY=sk_your_openai_api_key_here
DB_PASSWORD=123456
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local
```

**⚠️ Lưu ý**: `.env` đã được thêm vào `.gitignore` - không bao giờ commit!

---

## 🎯 So Sánh

| Lệnh Cũ | Lệnh Mới |
|----------|----------|
| `export GROQ_API_KEY=gsk_xxx && /Users/.../mvnw spring-boot:run` | `./run.sh` |
| Phải gõ export mỗi lần | Tự động load từ `.env` |
| Dài 100+ characters | 9 characters |

---

## 🛠️ Troubleshooting

### Script không chạy được
```bash
chmod +x run.sh dev.sh start.sh
```

### API key bị lỗi
Kiểm tra file `.env`:
```bash
cat .env | grep GROQ_API_KEY
```

### Port 8080 bị chiếm
Sửa trong `.env`:
```env
SERVER_PORT=8081
```

---

**Giờ chỉ cần gõ `./run.sh` là xong!** 🎉
