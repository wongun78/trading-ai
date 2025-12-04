# Enterprise Upgrade Guide - Trading AI

## 🎉 Full Enterprise Upgrade Completed!

Your Trading-AI project has been successfully upgraded with enterprise-grade features following the best practices from your teacher's CMS project.

---

## ✨ What's New

### 1. **Spring Security + JWT Authentication** 🔐

#### Features:
- JWT-based stateless authentication
- BCrypt password encryption
- Role-based access control (RBAC)
- Custom JWT filter
- Session management: STATELESS

#### Default Roles:
- **ROLE_ADMIN**: Full system access
- **ROLE_TRADER**: Create/manage own signals and positions
- **ROLE_VIEWER**: Read-only access

#### Default Admin Account:
```
Username: admin
Password: admin123
⚠️ Change password in production!
```

### 2. **Swagger/OpenAPI Documentation** 📝

Access interactive API documentation:
```
http://localhost:8080/swagger-ui.html
```

Features:
- Try-it-out functionality
- JWT authentication in UI
- Comprehensive schema documentation
- API versioning info

### 3. **Docker Deployment** 🐳

#### Quick Start:
```bash
# Copy environment file
cp .env.example .env

# Edit .env with your values
nano .env

# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

#### Services:
- **PostgreSQL 18.1**: Database
- **Spring Boot App**: Trading AI backend
- **Health checks**: Auto-restart on failure

### 4. **Role-Based Access Control** 🛡️

#### Endpoint Security:

| Endpoint | Access Level |
|----------|-------------|
| `POST /api/auth/login` | Public |
| `POST /api/auth/register` | Public |
| `GET /api/signals/**` | Public (read-only) |
| `POST /api/signals/ai-suggest` | TRADER, ADMIN |
| `POST /api/positions` | TRADER, ADMIN |
| `PUT /api/positions/{id}/close` | TRADER, ADMIN |
| `POST /api/admin/candles/**` | ADMIN only |
| `DELETE /api/admin/candles/**` | ADMIN only |

### 5. **Database Migration** 📊

New migration added:
- `V4__create_users_and_roles.sql`
- Creates `users`, `roles`, `user_roles` tables
- Initializes default roles
- Proper indexes and constraints

---

## 🚀 Getting Started

### 1. Setup Environment

```bash
# Copy environment template
cp .env.example .env

# Edit with your credentials
nano .env
```

### 2. Run Locally

```bash
# Build project
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Or use the run script
./run.sh
```

### 3. Run with Docker

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### 4. Test Authentication

#### Register New User:
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

#### Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

#### Use Token:
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

## 📋 API Documentation

### Swagger UI
- **URL**: http://localhost:8080/swagger-ui.html
- **Features**: 
  - Interactive API testing
  - JWT authentication
  - Request/response examples

### OpenAPI Spec
- **URL**: http://localhost:8080/v3/api-docs

---

## 🔧 Configuration

### Key Environment Variables

```bash
# Database
DB_PASSWORD=secure_password

# JWT (generate with: openssl rand -base64 64)
JWT_SECRET=your_base64_encoded_secret
JWT_EXPIRATION=86400000

# AI Services
GROQ_API_KEY=gsk_xxxxx
OPENAI_API_KEY=sk_xxxxx

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://your-frontend.com
```

### Spring Profiles

```bash
# Development
SPRING_PROFILES_ACTIVE=local

# Production
SPRING_PROFILES_ACTIVE=production
```

---

## 🎯 New Features Compared to Original

### Security (NEW):
- ✅ JWT authentication
- ✅ BCrypt password hashing
- ✅ Role-based access control
- ✅ Method-level security with `@PreAuthorize`
- ✅ CORS configuration

### Documentation (NEW):
- ✅ Swagger/OpenAPI UI
- ✅ API versioning
- ✅ Request/response schemas

### Deployment (NEW):
- ✅ Multi-stage Dockerfile
- ✅ Docker Compose orchestration
- ✅ Health checks
- ✅ Non-root container user

### Code Quality (ENHANCED):
- ✅ ApiResponse with message support
- ✅ ResourceAlreadyExistsException
- ✅ Swagger annotations on controllers
- ✅ Security annotations on endpoints

### Database (ENHANCED):
- ✅ User and Role entities
- ✅ Flyway migration for auth tables
- ✅ Data initialization for roles

---

## 🏗️ Architecture

### New Components:

```
src/main/java/fpt/wongun/trading_ai/
├── config/
│   ├── JwtProperties.java          # JWT configuration
│   ├── JwtFilter.java              # JWT authentication filter
│   ├── SecurityConfig.java         # Spring Security config
│   ├── OpenApiConfig.java          # Swagger configuration
│   └── DataInitializer.java        # Seed data
├── controller/
│   └── AuthController.java         # Login/register endpoints
├── domain/entity/
│   ├── User.java                   # User entity
│   └── Role.java                   # Role entity
├── dto/auth/
│   ├── LoginRequestDto.java
│   ├── LoginResponseDto.java
│   └── RegisterRequestDto.java
├── repository/
│   ├── UserRepository.java
│   └── RoleRepository.java
├── service/
│   ├── AuthService.java            # Authentication logic
│   ├── TokenService.java           # JWT token management
│   └── CustomUserDetailsService.java # User loading
└── exception/
    └── ResourceAlreadyExistsException.java
```

---

## 🔐 Security Best Practices

### Production Checklist:

- [ ] Change default admin password
- [ ] Generate secure JWT secret: `openssl rand -base64 64`
- [ ] Use environment variables (never hardcode secrets)
- [ ] Enable HTTPS/SSL
- [ ] Configure proper CORS origins
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Disable `show-sql` in production
- [ ] Set up rate limiting
- [ ] Configure session timeout
- [ ] Enable security headers
- [ ] Set up monitoring and alerting

---

## 📊 Comparison: Before vs After

| Feature | Before | After |
|---------|--------|-------|
| Authentication | ❌ None | ✅ JWT + Spring Security |
| Authorization | ❌ None | ✅ Role-based (ADMIN/TRADER/VIEWER) |
| API Documentation | ❌ None | ✅ Swagger/OpenAPI |
| Docker Support | ⚠️ Basic scripts | ✅ Full Docker Compose |
| Security | ⚠️ Open endpoints | ✅ Protected with roles |
| User Management | ❌ None | ✅ Full CRUD + registration |
| Password Encryption | ❌ N/A | ✅ BCrypt |
| CORS | ⚠️ Default | ✅ Configurable |
| Health Checks | ⚠️ Basic actuator | ✅ Docker + actuator |

---

## 🎓 Learning from Teacher's Code

### What We Adopted:

1. **JWT Implementation** - Token-based auth
2. **Security Architecture** - Filter chain + method security
3. **Swagger Integration** - API documentation
4. **Docker Deployment** - Production-ready containers
5. **RBAC Pattern** - Role-based access control
6. **ApiResponse Enhancement** - Message support
7. **Data Initialization** - Seed default data

### What We Kept (Already Better):

1. **BaseEntity Pattern** - Auto auditing (teacher's code lacks this)
2. **Flyway Migrations** - Version-controlled DB changes
3. **Soft Delete** - Data recovery capability
4. **Optimistic Locking** - Concurrency control
5. **Type-safe Enums** - Rich domain models
6. **Validation Framework** - Bean validation

---

## 🐛 Troubleshooting

### Common Issues:

1. **Port already in use**
   ```bash
   # Change port in .env
   SERVER_PORT=8081
   ```

2. **Database connection failed**
   ```bash
   # Check PostgreSQL is running
   docker-compose ps
   
   # Check credentials in .env
   DB_PASSWORD=correct_password
   ```

3. **JWT validation failed**
   ```bash
   # Ensure JWT_SECRET is base64 encoded
   # Generate new: openssl rand -base64 64
   ```

4. **CORS errors**
   ```bash
   # Add your frontend URL to .env
   CORS_ALLOWED_ORIGINS=http://localhost:3000
   ```

---

## 📚 Next Steps

### Recommended Enhancements:

1. **Testing**
   - Unit tests for services
   - Integration tests for controllers
   - Security tests

2. **Monitoring**
   - Prometheus metrics
   - Grafana dashboards
   - ELK stack for logs

3. **Performance**
   - Redis caching
   - Connection pooling optimization
   - Query optimization

4. **Features**
   - Password reset flow
   - Email verification
   - Two-factor authentication
   - User profile management

---

## 📞 Support

For questions or issues:
- Check Swagger UI for API details
- Review application logs
- See README.md for general info

**Happy Trading! 🚀**
