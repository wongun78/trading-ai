# Security Integration Guide

## Overview

This document explains how the new authentication and authorization modules integrate with existing business logic.

## What Changed?

### 1. **User Tracking (BaseEntity Auditing)**

**Before:**
```java
// JpaAuditConfig.java
return Optional.of("SYSTEM"); // Hard-coded placeholder
```

**After:**
```java
// JpaAuditConfig.java
return Optional.of(securityUtils.getCurrentUsername()); // Real authenticated user
```

**Impact:**
- `created_by` and `last_modified_by` now track real usernames
- Every Position, AiSignal saved automatically records who created/modified it
- Audit trail for compliance and debugging

---

### 2. **Ownership Validation**

**Before:**
```java
// PositionService.java
public PositionResponseDto closePosition(Long id, ClosePositionRequestDto request) {
    Position position = positionRepository.findById(id).orElseThrow(...);
    // ❌ Anyone can close anyone's position!
    position.setStatus(PositionStatus.CLOSED);
    ...
}
```

**After:**
```java
// PositionService.java
public PositionResponseDto closePosition(Long id, ClosePositionRequestDto request) {
    Position position = positionRepository.findById(id).orElseThrow(...);
    
    validateOwnership(position); // ✅ Check owner or admin
    
    position.setStatus(PositionStatus.CLOSED);
    ...
}

private void validateOwnership(Position position) {
    if (securityUtils.isAdmin()) {
        return; // Admin can access all
    }
    
    String currentUser = securityUtils.getCurrentUsername();
    if (!currentUser.equals(position.getCreatedBy())) {
        throw new InvalidPositionException("You can only manage your own positions");
    }
}
```

**Impact:**
- Users can only close/execute/cancel their own positions
- Admins bypass ownership checks (full access)
- Prevents unauthorized data modification

---

### 3. **Data Filtering (Query Scoping)**

**Before:**
```java
// PositionService.java
public Page<PositionResponseDto> getPositions(...) {
    // ❌ Returns ALL positions for everyone
    return positionRepository.findAll(pageable);
}
```

**After:**
```java
// PositionService.java
public Page<PositionResponseDto> getPositions(...) {
    if (!securityUtils.isAdmin()) {
        String currentUser = securityUtils.getCurrentUsername();
        // ✅ Filter by user - only see own positions
        return positionRepository.findByCreatedByOrderByOpenedAtDesc(currentUser, pageable);
    }
    // Admin sees all positions
    return positionRepository.findAll(pageable);
}
```

**Impact:**
- Users see only their own positions in lists
- Admins see all positions (for monitoring/support)
- Data isolation between users

---

## Key Components

### SecurityUtils Helper

Location: `fpt.wongun.trading_ai.util.SecurityUtils`

**Purpose:** Bridge between Spring Security and business logic

**Methods:**
```java
// Get current authenticated username
String username = securityUtils.getCurrentUsername();
// Returns "SYSTEM" if no auth context (e.g., background jobs)

// Get full User entity
User user = securityUtils.getCurrentUser();

// Check if current user is admin
boolean isAdmin = securityUtils.isAdmin();

// Check specific role
boolean isTrader = securityUtils.hasRole("TRADER");
```

**Usage Pattern:**
```java
@Service
@RequiredArgsConstructor
public class PositionService {
    private final SecurityUtils securityUtils; // Inject
    
    public void someMethod() {
        String currentUser = securityUtils.getCurrentUsername();
        // Use currentUser for filtering, validation, etc.
    }
}
```

---

## Updated Services

### ✅ PositionService

**Ownership Validation:**
- `executePosition()` - Check owner before filling order
- `closePosition()` - Check owner before closing
- `cancelPosition()` - Check owner before cancelling
- `getPosition()` - Check owner before viewing single position

**Data Filtering:**
- `getPositions()` - Filter by user (unless admin)
- `getOpenPositions()` - Already filtered by user param
- `getPortfolioStats()` - Already scoped to user param

**Auditing:**
- `openPosition()` - BaseEntity automatically sets `created_by`
- All updates - BaseEntity sets `last_modified_by`

---

### ⚠️ AiSignalService (TODO)

**Needs Update:**
```java
// TODO: Associate signals with user
public AiSignal generateSignal(...) {
    // Add: String username = securityUtils.getCurrentUsername();
    // Signals should be tied to users for personalized strategies
}
```

---

## Repository Changes

### PositionRepository

**New Query Methods:**
```java
// Filter positions by user
Page<Position> findByCreatedByOrderByOpenedAtDesc(String createdBy, Pageable pageable);

// Filter by user + symbol
Page<Position> findByCreatedByAndSymbolOrderByOpenedAtDesc(
    String createdBy, Symbol symbol, Pageable pageable);

// Filter by user + symbol + status
Page<Position> findByCreatedByAndSymbolAndStatusOrderByOpenedAtDesc(
    String createdBy, Symbol symbol, PositionStatus status, Pageable pageable);
```

**Why:** Spring Data JPA auto-generates SQL based on method names - no manual queries needed!

---

## Role-Based Access Control (RBAC)

### Roles Hierarchy

```
ROLE_ADMIN (Full access)
  ├── Can view/edit ALL positions
  ├── Can manage users
  └── Can access admin endpoints
  
ROLE_TRADER (Limited)
  ├── Can create positions
  ├── Can view/edit OWN positions only
  └── Cannot access admin functions
  
ROLE_VIEWER (Read-only)
  ├── Can view OWN positions
  └── Cannot create/edit positions
```

### Controller Protection

```java
@RestController
@RequestMapping("/api/positions")
public class PositionController {

    @PostMapping
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PositionResponseDto>> openPosition(...) {
        // ✅ Only TRADER or ADMIN can create positions
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()") // All authenticated users
    public ResponseEntity<Page<PositionResponseDto>> getPositions(...) {
        // ✅ Service layer filters by user
    }
}
```

### Service Layer Protection

```java
@Service
public class PositionService {
    
    private void validateOwnership(Position position) {
        // ✅ Double protection at service layer
        // Even if controller bypassed, service validates
    }
}
```

**Defense in Depth:** Controller checks role, Service checks ownership

---

## Testing the Integration

### 1. Login as Regular User

```bash
# Login as trader
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "trader1", "password": "pass123"}'

# Response: {"token": "eyJhbGciOiJIUzI1NiJ9..."}
```

### 2. Open Position

```bash
curl -X POST http://localhost:8080/api/positions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbolCode": "BTCUSDT",
    "direction": "LONG",
    "quantity": 0.01,
    "entryPrice": 45000,
    "stopLoss": 44000,
    "takeProfit": 46000
  }'
```

**Expected:** Position created with `created_by = "trader1"`

### 3. List Positions

```bash
curl -X GET http://localhost:8080/api/positions \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:** Only see positions where `created_by = "trader1"`

### 4. Try to Access Other User's Position

```bash
# Get position ID from another user (e.g., ID=999)
curl -X GET http://localhost:8080/api/positions/999 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:** 
```json
{
  "success": false,
  "message": "You can only manage your own positions. Position belongs to: trader2",
  "timestamp": "2024-12-03T10:00:00Z"
}
```

### 5. Login as Admin

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### 6. Admin Sees All Positions

```bash
curl -X GET http://localhost:8080/api/positions \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Expected:** See ALL positions from all users

---

## Migration Path for Existing Data

### Problem
Existing positions have `created_by = "SYSTEM"` from before authentication was added.

### Solution Options

#### Option 1: Assign to Admin
```sql
-- Assign all orphaned positions to admin
UPDATE positions 
SET created_by = 'admin', last_modified_by = 'admin'
WHERE created_by = 'SYSTEM';
```

#### Option 2: Create Migration User
```sql
-- Create a special "migration" user
INSERT INTO users (username, email, password, enabled)
VALUES ('migration', 'migration@system.local', 'LOCKED', false);

-- Assign old data to migration user
UPDATE positions 
SET created_by = 'migration'
WHERE created_by = 'SYSTEM';
```

#### Option 3: Leave as System
```
-- Do nothing
-- Admin can still view/manage these positions
-- Future data will have proper owners
```

**Recommended:** Option 1 (assign to admin) for simplicity

---

## Common Issues

### Issue 1: "Access Denied" on Authenticated Requests

**Symptom:**
```json
{"success": false, "message": "Access Denied"}
```

**Cause:** Token missing or invalid

**Fix:**
```bash
# Check Authorization header format
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
#             ^^^^^^ Space required!
```

### Issue 2: See "SYSTEM" in created_by

**Symptom:** New positions still show `created_by = "SYSTEM"`

**Cause:** Not authenticated or JpaAuditConfig not updated

**Fix:**
1. Verify login: `POST /api/auth/login`
2. Include token in request header
3. Check JpaAuditConfig uses SecurityUtils

### Issue 3: Can't See Any Positions

**Symptom:** `GET /api/positions` returns empty array

**Cause:** Filtering by username, no positions for this user

**Fix:**
1. Check you opened positions with this account
2. Login as admin to see all positions
3. Check database: `SELECT * FROM positions WHERE created_by = 'your_username';`

---

## Best Practices

### ✅ DO

1. **Always inject SecurityUtils** in services that need user context
2. **Validate ownership** in service layer (defense in depth)
3. **Filter queries by user** unless admin
4. **Use @PreAuthorize** on controller methods
5. **Check isAdmin()** before showing all users' data

### ❌ DON'T

1. **Don't bypass ownership checks** - even for convenience
2. **Don't hard-code usernames** - use SecurityUtils
3. **Don't trust client-side** - always validate server-side
4. **Don't expose other users' data** in error messages
5. **Don't skip authentication** for "internal" endpoints

---

## Future Enhancements

### 1. Position Sharing
Allow users to share positions with team members:
```java
@ManyToMany
private Set<User> sharedWith;
```

### 2. Portfolio Managers
Traders can assign "managers" who can view (but not edit) their positions:
```java
@ManyToOne
private User portfolioManager;
```

### 3. Audit Logging
Log all position modifications for compliance:
```java
@EntityListeners(AuditingEntityListener.class)
public class PositionAuditLog {
    private String action; // OPEN, CLOSE, MODIFY
    private String performedBy;
    private Instant performedAt;
}
```

---

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Auditing** | `created_by = "SYSTEM"` | `created_by = "alice"` |
| **Ownership** | No validation | Users edit only own data |
| **Filtering** | See all positions | See own positions |
| **Admin Role** | N/A | Full access to all data |
| **Security** | Open access | Role-based + ownership |

**Result:** Multi-tenant trading platform with proper data isolation! 🎉

---

## Related Documentation

- [ENTERPRISE_UPGRADE.md](./ENTERPRISE_UPGRADE.md) - Initial security setup
- [QUICKSTART.md](./QUICKSTART.md) - How to run with authentication
- [SecurityUtils.java](./src/main/java/fpt/wongun/trading_ai/util/SecurityUtils.java) - Helper class
- [PositionService.java](./src/main/java/fpt/wongun/trading_ai/service/PositionService.java) - Integration example
