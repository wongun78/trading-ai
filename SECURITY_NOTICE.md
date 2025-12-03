# ⚠️ SECURITY NOTICE: GROQ API KEY LEAKED

## Issue
A Groq API key was found in `application-local.yml` (local development file).

## Immediate Actions Required

### 1. Rotate the API Key (URGENT)
1. Go to https://console.groq.com
2. Navigate to API Keys section
3. **Revoke/Delete** the old key that was in `application-local.yml`
4. Create a new API key
5. Update your local `.env` file with the new key

### 2. Update Your Local Configuration
Add to your `.env` file (create if not exists):
```bash
GROQ_API_KEY=your_new_key_here
DB_PASSWORD=123456
```

### 3. Verify Security
```bash
# Make sure these files are NOT tracked by git
git ls-files | grep -E "\.env$|application-local\.yml"
# Should return nothing

# Verify .gitignore
cat .gitignore | grep -E "\.env|application-local"
```

## What Was Fixed

✅ Removed all secrets from `application-local.yml`  
✅ Removed default secret values from `application.yml`  
✅ Updated `.env.example` with clear documentation  
✅ Created `ProductionConfigValidator` to prevent deploying with placeholder secrets  

## Configuration Best Practices

### File Purposes

**application.yml** (committed to git):
- Contains structure and environment variable references
- NO actual secrets, only `${ENV_VAR}` placeholders
- Production-safe defaults

**application-local.yml** (ignored by git):
- Override configuration for local development ONLY
- NO secrets - only non-sensitive overrides
- Enable debug logging, etc.

**.env** (ignored by git):
- ALL secrets go here
- Never commit this file
- Each developer has their own

**.env.example** (committed to git):
- Template showing required variables
- Contains placeholder values only

### Example Usage

1. Copy template:
```bash
cp .env.example .env
```

2. Edit `.env` with your actual values:
```bash
GROQ_API_KEY=gsk_your_actual_key_here
DB_PASSWORD=your_password
```

3. Start application:
```bash
./run.sh  # or ./dev.sh
```

## Production Deployment

When deploying to production (Heroku, AWS, etc.):

1. Set environment variables in your hosting platform
2. Set `SPRING_PROFILES_ACTIVE=prod`
3. The `ProductionConfigValidator` will verify all required secrets are set
4. Application will fail to start if any placeholder values are detected

## Never Commit
- `.env`
- `.env.local`
- `application-local.yml`
- Any file containing actual API keys or passwords
