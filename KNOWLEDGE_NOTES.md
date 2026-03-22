# JWT Access Token & Refresh Token

## What Are They?

After a successful login, the API returns two tokens:

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "2db14814-eb24-4580-b4e4-86d739a67d1a",
  "expiresIn": 900
}
```

---

## Access Token

**What it is:** A JWT (JSON Web Token) — self-contained, cryptographically signed, short-lived.

**Decoded payload:**
```json
{
  "sub": "user@gmail.com",
  "userId": "3861fb29-7e34-4921-9a1c-c3916114ea5d",
  "email": "user@gmail.com",
  "role": "CUSTOMER",
  "iat": 1773684691,
  "exp": 1773685591
}
```

**Where to use it:** Every protected API call as an `Authorization` header:
```bash
curl http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGci..."
```

**Lifetime:** 15 minutes (`expiresIn: 900`). Gateway rejects it with `401` after expiry.

**Who validates it:** The API Gateway (`JwtAuthFilter`) on every non-public request — checks signature, expiry, and Redis blacklist.

**Downstream services:** Never parse the JWT. They only read `X-User-Id`, `X-User-Role`, `X-User-Email` headers injected by the gateway.

---

## Refresh Token

**What it is:** A random UUID stored in Redis as `refresh:{userId}` → token value. No data inside — just a lookup key.

**Lifetime:** 30 days.

**Where to use it:** When the access token expires, call the refresh endpoint to get a new access token **without logging in again:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "3861fb29-7e34-4921-9a1c-c3916114ea5d",
    "refreshToken": "2db14814-eb24-4580-b4e4-86d739a67d1a"
  }'
```

---

## The Full Flow

```
Login → get accessToken (15 min) + refreshToken (30 days)
         ↓
Use accessToken on every API call via Authorization: Bearer header
         ↓
accessToken expires after 15 min → Gateway returns 401
         ↓
Call /auth/refresh with refreshToken → get a new accessToken
         ↓
refreshToken expires after 30 days → user must login again
```

---

## Why Two Tokens?

| | Access Token | Refresh Token |
|---|---|---|
| Lifetime | 15 min | 30 days |
| Storage | Client memory / header | Client storage (secure) |
| Used on | Every API call | Only to get new access token |
| Validated by | Gateway (JWT signature + Redis blacklist) | Auth-service (Redis lookup) |
| If stolen | Expires in max 15 min | Can be revoked in Redis |

Short-lived access token limits damage if intercepted. Long-lived refresh token prevents user from having to log in every 15 minutes.

---

## Logout Behaviour

On logout, auth-service does two things:
1. Adds `blacklist:{accessToken}` to Redis with TTL = remaining access token lifetime
2. Deletes `refresh:{userId}` from Redis

After logout, any request with the old access token → gateway finds it in blacklist → `401` immediately.

---

## JWT Blacklisting — How It Works and Why Two JWTs Can Never Be the Same

### Why JWT Is Stateless By Design
JWT is **stateless** — once issued, it is valid until its expiry time. There is no built-in mechanism to invalidate a JWT mid-lifetime. The server does not keep track of issued tokens anywhere. This is what makes JWT scalable — no server-side session storage. But it also means that if a user logs out, the token is still mathematically valid until it expires.

The **Redis blacklist** is the standard solution to this problem.

---

### How the Blacklist Works on Logout

```java
public void logout(String userId, String accessToken) {
    tokenService.deleteRefreshToken(userId);         // Step 1 — delete refresh:{userId}
    long expiry = jwtUtil.extractAllClaims(accessToken).getExpiration().getTime();
    long ttl = (expiry - System.currentTimeMillis()) / 1000;  // remaining seconds
    if (ttl > 0) tokenService.blacklistToken(accessToken, ttl); // Step 2 — blacklist with TTL
}
```

**Step 1 — Delete Refresh Token:**
`refresh:{userId}` is removed from Redis. The user cannot silently get a new access token anymore. Even if the blacklisted access token expires, no new one can be issued without logging in again.

**Step 2 — Blacklist Access Token with Remaining TTL:**
The access token is stored in Redis as `blacklist:{token}` with TTL = remaining lifetime.

Example:
```
Token issued at  10:00  (valid for 15 min, expires 10:15)
User logs out at 10:10
TTL = 10:15 - 10:10 = 5 minutes

Redis stores: blacklist:{token} with TTL = 5 min
At 10:15 → token would have expired anyway → Redis auto-deletes the key
```

This is efficient — the key is never stored longer than necessary. No manual cleanup job needed.

---

### Gateway Checks Blacklist on Every Request

```java
// JwtAuthFilter.java
return redisTemplate.hasKey("blacklist:" + token)
        .flatMap(blacklisted -> {
            if (Boolean.TRUE.equals(blacklisted)) {
                return unauthorised(exchange, "Token has been invalidated");  // 401
            }
            return chain.filter(mutatedExchange);  // pass through
        });
```

The blacklist check happens **after** signature and expiry validation. So the order is:
```
Request arrives at gateway
      ↓
1. Is token signature valid?           → NO  → 401
      ↓
2. Is token expired?                   → YES → 401
      ↓
3. Is token in Redis blacklist?        → YES → 401
      ↓
Pass through to downstream service
```

---

### Can Two JWTs Ever Be Identical?

**No — it is cryptographically impossible.** Here is why:

Every JWT payload contains an `iat` (issued-at) claim — the exact Unix timestamp in seconds at the moment of generation:

```json
{
  "userId": "3861fb29-7e34-4921-9a1c-c3916114ea5d",
  "email":  "user@gmail.com",
  "role":   "CUSTOMER",
  "iat":    1773684691,     ← changes every login
  "exp":    1773685591
}
```

The JWT signature is computed as:
```
HMAC-SHA256(base64(header) + "." + base64(payload), secretKey) = signature
```

Since `iat` changes with every login, the payload bytes are always different → HMAC-SHA256 produces a completely different signature → the full JWT string is always unique, even for the same user logging in twice.

Even in the theoretical scenario where two logins happen within the same second (same `iat`), the HMAC function guarantees that any difference in input — even a single bit — produces a completely different output (avalanche effect). So identical JWTs are not possible.

---

### Same User Logged In on Multiple Devices

Since every login produces a unique JWT, multiple active sessions are fully independent:

```
User logs in on Phone   → JWT_A  (iat = 1773684691)
User logs in on Laptop  → JWT_B  (iat = 1773684695)
User logs in on Tablet  → JWT_C  (iat = 1773684712)

User logs out on Phone  → blacklist:JWT_A added to Redis
                        → JWT_B and JWT_C are completely unaffected
                        → Phone session blocked, Laptop and Tablet still work
```

Logging out on one device does **not** affect other devices. The blacklist operates on the exact token string — not on the userId.

---

### "Logout From All Devices" — Future Enhancement

The current implementation only blacklists the single token presented at logout. To implement "logout everywhere":
1. Store all active tokens per userId in Redis: `sessions:{userId}` → Set of tokens
2. On logout-all → iterate the set → blacklist every token → clear the set

This is not currently implemented but is a natural extension of the existing blacklist mechanism.

---

### Summary Table

| Scenario | What Happens |
|----------|-------------|
| Valid token, active session | No blacklist entry → request passes through |
| Token presented after logout | `blacklist:{token}` found in Redis → 401 |
| Token expired naturally | JWT expiry check fails at gateway → 401 (blacklist never reached) |
| Token expired AND blacklisted | Redis TTL already auto-deleted the key — JWT expiry check catches it |
| Same user logs in twice | Two completely different JWT strings — independent sessions |
| Logout on Device A | Only Device A's token blacklisted — Device B unaffected |

---

# BCrypt Password Hashing

## What is BCrypt?

BCrypt is a **password hashing function** designed by Niels Provos and David Mazières in 1999, based on the **Blowfish symmetric-key block cipher**. It was specifically designed for password storage with three properties:

1. **One-way** — output cannot be reversed to get the input
2. **Salted** — random salt built-in, defeats pre-computation attacks
3. **Adaptive cost** — deliberately slow, cost factor can be increased as hardware gets faster

---

## The Blowfish Cipher — What BCrypt is Built On

Blowfish is a **symmetric block cipher** that operates on 64-bit blocks using a variable-length key (32 bits to 448 bits). BCrypt uses a modified version called **Eksblowfish** (Expensive Key Schedule Blowfish).

The key insight: Blowfish has an **expensive key setup phase** — initialising its subkeys and S-boxes requires many iterations. BCrypt exploits this deliberately:

```
Standard encryption: key setup once → encrypt fast
BCrypt:              key setup repeated 2^cost times → deliberately slow
```

### Internal BCrypt Algorithm Steps

```
Input: password (plain text), salt (128-bit random), cost (integer)

Step 1 — EksBlowfishSetup(cost, salt, password)
   → Initialise 18 subkeys (P-array) with digits of Pi
   → Initialise 4 S-boxes (256 entries each) with more digits of Pi
   → XOR subkeys with password bytes (cycling if password < key length)
   → Run key schedule loop 2^cost times using salt and password alternately

Step 2 — Encrypt the magic string "OrpheanBeholderScryDoubt" 64 times
   using the Blowfish cipher with the derived key state from Step 1

Step 3 — Output: cost + salt + encrypted result = final BCrypt hash
```

The magic string `"OrpheanBeholderScryDoubt"` is a fixed 24-byte plaintext encrypted 64 times — its ciphertext becomes the hash output.

---

## How Passwords Are Stored

Passwords are **never stored as plain text** — BCrypt hashes them before saving to MongoDB.

```java
// On signup — encode before saving
.password(passwordEncoder.encode(req.getPassword()))

// On login — compare without storing plain text
passwordEncoder.matches(req.getPassword(), user.getPassword())

// On password reset — encode new password before saving
user.setPassword(passwordEncoder.encode(req.getNewPassword()));
```

BCrypt is configured in `SecurityConfig.java`:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // default cost = 10
}
```

What gets saved in MongoDB `auth_db.users`:
```json
{ "password": "$2a$10$xK8v2mN3pQrLwJhT5uYeZuOkVb1Lm9Nq4Rj7Sc2Td8Ue6Wf0Yg3Hi" }
```

---

## BCrypt Hash Structure

```
$2a$10$xK8v2mN3pQrLwJhT5uYeZu  OkVb1Lm9Nq4Rj7Sc2Td8Ue6Wf0Yg3Hi
─────  ──  ──────────────────── ───────────────────────────────────
algo  cost    salt (22 chars)        actual hash (31 chars)
```

| Part | Value | Meaning |
|------|-------|---------|
| `$2a$` | algorithm version | BCrypt version 2a |
| `10` | cost factor | 2^10 = 1024 key setup iterations |
| next 22 chars | salt | 128-bit random salt, Base64 encoded |
| last 31 chars | hash | encrypted magic string, Base64 encoded |

The **salt is embedded inside the stored hash string itself** — no separate storage needed.

---

## How `matches()` Works (Login)

```java
passwordEncoder.matches("MyPassword123", "$2a$10$xK8v2mN3pQrLwJhT5uYeZuOkVb1Lm9Nq4Rj7Sc2Td8Ue6Wf0Yg3Hi")
```

Internally:
```
Step 1 → Extract salt from stored hash
         salt = "xK8v2mN3pQrLwJhT5uYeZu"  (characters 7–28)

Step 2 → Re-hash incoming password using that SAME extracted salt + same cost
         result = BCrypt("MyPassword123", salt="xK8v2mN3pQrLwJhT5uYeZu", cost=10)
                = "$2a$10$xK8v2mN3pQrLwJhT5uYeZuOkVb1Lm9Nq4Rj7Sc2Td8Ue6Wf0Yg3Hi"

Step 3 → Compare full strings
         stored   = "$2a$10$xK8v2...OkVb1..."
         computed = "$2a$10$xK8v2...OkVb1..."
                    ─────────────────────────
                         MATCH ✅ → return true
```

Key point: The salt is **not randomly generated again** on login — it is extracted from the stored hash. The same salt + same password always produces the same hash, which is why comparison works.

---

## Why an Adversary Cannot Recover the Plain Text from a BCrypt Hash

### 1. Pre-Image Resistance (One-Way Property)
```
"MyPassword123"   →  BCrypt  →  "$2a$10$...Hash"   ✅ computationally easy
"$2a$10$...Hash"  →  ???     →  "MyPassword123"    ❌ computationally infeasible
```
BCrypt is a **cryptographic one-way function**. The Blowfish cipher's key schedule is designed so that knowing the output gives no mathematical shortcut to the input. The only viable strategy for an adversary is exhaustive search.

### 2. Exhaustive Search / Brute Force Attack
An adversary's only option is to guess passwords one by one:
```
Attempt 1: BCrypt("password123", extracted_salt, cost=10) → compare → no match
Attempt 2: BCrypt("admin123",    extracted_salt, cost=10) → compare → no match
Attempt 3: BCrypt("MyPassword123", extracted_salt, cost=10) → compare → MATCH ✅
```

BCrypt makes this **deliberately slow** — cost factor 10 means 2^10 = 1024 Eksblowfish key setup iterations per hash attempt:
- ~100ms per attempt on modern hardware
- ~10 attempts/second maximum per CPU core

| Password type | Example | Time to exhaust |
|--------------|---------|----------------|
| Common dictionary word | `password` | < 1 second (known list) |
| Simple + number | `john1234` | Minutes |
| Random alphanumeric 8 chars | `xK8v2mN3` | ~100 years |
| Strong mixed 12 chars | `MyP@ssw0rd!2` | Millions of years |

### 3. Pre-Computation Resistance (Rainbow Table Attack Defeated)
A **Rainbow Table** is a pre-computed lookup database mapping `hash → plaintext` for millions of known passwords. This makes MD5/SHA1 password hashes trivially crackable.

BCrypt's per-user random salt completely defeats this:
```
"password" + salt_A  →  "$2a$10$AAAA...Hash1"
"password" + salt_B  →  "$2a$10$BBBB...Hash2"
"password" + salt_C  →  "$2a$10$CCCC...Hash3"
```
Same password, completely different hashes. An adversary would need to pre-compute a separate rainbow table for every possible 128-bit salt — requiring 2^128 precomputed entries, which is physically impossible.

### 4. Hash Isolation Per User
Even if two users share the same password, their hashes differ (different salts generated at signup):
```
User A: "Test@1234"  →  "$2a$10$xK8v2...AbcHash"
User B: "Test@1234"  →  "$2a$10$pQ9r3...XyzHash"
```
Compromising one user's hash gives an adversary **zero information** about any other user's password.

---

## BCrypt vs Other Hashing Algorithms

| Algorithm | Salted | Slow by design | Recommended for passwords |
|-----------|--------|---------------|--------------------------|
| MD5 | No | No (~ns/hash) | ❌ Broken |
| SHA-1 | No | No (~ns/hash) | ❌ Broken |
| SHA-256 | No | No (~ns/hash) | ❌ Too fast for passwords |
| BCrypt | Yes (128-bit) | Yes (cost factor) | ✅ |
| Argon2 | Yes | Yes (memory-hard) | ✅ Modern alternative |
| scrypt | Yes | Yes (memory-hard) | ✅ Modern alternative |

BCrypt is the standard for Spring Security. Argon2 is the newer recommendation (winner of Password Hashing Competition 2015) but BCrypt remains secure for password storage.

---

## When BCrypt Can Be Compromised

| Scenario | Risk |
|----------|------|
| User chose `password123` or `123456` | Dictionary attack succeeds instantly |
| Cost factor too low (e.g. `cost=4`) | Hashing is fast → exhaustive search becomes feasible |
| Same salt reused for all users | Rainbow table can be pre-computed once and reused |
| Password leaked in plain text elsewhere | BCrypt irrelevant — the plain text is already exposed |

This is why `SignupRequest` enforces strong password rules via `@Pattern` validation — weak passwords are rejected before they ever reach BCrypt.

---

## Salt Lifecycle Summary

| Moment | Salt behaviour |
|--------|---------------|
| Signup (`encode()`) | 128-bit random salt generated once, embedded in stored hash |
| Login (`matches()`) | Salt extracted from stored hash — reused, NOT regenerated |
| Password Reset (`encode()`) | New 128-bit random salt generated → completely different hash even for same password |

---

# Eureka — Service Discovery

## What is Eureka?

Eureka is a **service discovery server** from Netflix, integrated into Spring Cloud. It acts as a central registry where every microservice registers itself on startup and looks up other services by name at runtime.

In simple terms: **Eureka is the phone book of your microservices.**

---

## The Problem It Solves

In this project there are 12 microservices, each running on a different port. Services need to call each other — for example, `order-service` calls `inventory-service` to reserve stock.

### Without Eureka (hardcoded addresses)

```yaml
# order-service application.yml
inventory-service-url: http://localhost:8084
payment-service-url:   http://localhost:8086
```

This breaks the moment you:
- Deploy to Docker — `localhost` means nothing inside a container; each container gets its own dynamic IP
- Scale a service — run 2 instances of `inventory-service` on different ports; which one do you call?
- Change a port — every service that calls it needs to be updated manually

### With Eureka (dynamic discovery)

Every service registers itself with Eureka on startup:

```
inventory-service  →  "I'm alive at 172.18.0.5:8084"
payment-service    →  "I'm alive at 172.18.0.9:8086"
cart-service       →  "I'm alive at 172.18.0.7:8087"
```

When `order-service` wants to call `inventory-service`, it asks Eureka for the current address — no hardcoding needed.

---

## How It Works (Step by Step)

```
1. Eureka Server starts → dashboard available at localhost:8761

2. Each microservice starts → registers with Eureka:
   POST http://eureka-server:8761/eureka/apps/INVENTORY-SERVICE
   Body: { host, port, status: "UP" }

3. Eureka stores the registry:
   INVENTORY-SERVICE  →  172.18.0.5:8084
   PAYMENT-SERVICE    →  172.18.0.9:8086
   ...

4. Every 30 seconds, each service sends a heartbeat to Eureka:
   PUT http://eureka-server:8761/eureka/apps/INVENTORY-SERVICE/instance-id
   → Proves it is still alive

5. If heartbeat stops → Eureka removes the service from registry after ~90 seconds
   → No traffic is routed to a dead instance

6. When order-service needs to call inventory-service:
   → Asks Eureka: "Where is INVENTORY-SERVICE?"
   → Gets back: "172.18.0.5:8084"
   → Makes the call
```

---

## Usage in This Project

### 1. API Gateway routes use `lb://` (load-balanced URI)

```yaml
# api-gateway application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: inventory-service
          uri: lb://inventory-service    # lb = ask Eureka, then load balance
          predicates:
            - Path=/api/inventory/**
          filters:
            - StripPrefix=1
```

`lb://` tells Spring Cloud Gateway to resolve the service name through Eureka at request time.

### 2. Feign clients use service name, not URL

```java
// order-service calling inventory-service
@FeignClient(name = "inventory-service")   // service name registered in Eureka
public interface InventoryClient {

    @PostMapping("/inventory/reserve")
    ReserveResponse reserveStock(@RequestBody ReserveRequest request);

    @PostMapping("/inventory/release")
    void releaseStock(@RequestBody ReleaseRequest request);
}
```

No `localhost:8084` anywhere. Spring Cloud LoadBalancer asks Eureka for the current address and picks an instance if multiple are running.

### 3. Every service registers on startup

```yaml
# application.yml — same pattern for all services
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true
```

```java
// Main application class of every service
@SpringBootApplication
@EnableDiscoveryClient          // registers this service with Eureka
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
```

### 4. Eureka Server setup

```java
// eureka-server main class
@SpringBootApplication
@EnableEurekaServer             // turns this app into the registry
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# eureka-server application.yml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false   # server does not register itself
    fetch-registry: false         # server does not fetch registry from another server
  server:
    wait-time-in-ms-when-sync-empty: 0
```

---

## What Eureka Gives You

| Problem | How Eureka Solves It |
|---|---|
| Services deployed in Docker with dynamic IPs | Services register by name; actual IP looked up at runtime |
| Running 2 instances of a service | Eureka holds both; load balancer picks one (round-robin) |
| A service crashes | Heartbeat stops → removed from registry → no traffic sent to dead instance |
| New service added | Just starts, registers automatically — no config change elsewhere |
| Port changes | Only the service itself needs to update its config |

---

## Feign Clients in This Project

Every synchronous inter-service call goes through Eureka:

| Caller | Calls | Purpose |
|---|---|---|
| `api-gateway` | all services | route + load balance all incoming requests |
| `order-service` | `inventory-service` | reserve / release / confirm stock |
| `order-service` | `coupon-service` | validate coupon + record usage |
| `order-service` | `cart-service` | fetch cart contents on checkout |
| `cart-service` | `product-service` | get current price + availability |
| `cart-service` | `coupon-service` | validate promo code |
| `review-service` | `product-service` | update avgRating + reviewCount |
| `review-service` | `order-service` | verify order is DELIVERED before allowing review |
| `delivery-service` | `user-service` | get customer delivery address |

---

## Service Registry at Runtime

When all services are running you can view the live registry at:

```
http://localhost:8761
```

You will see all registered services, their instance IDs, IP addresses, ports, and health status.

---

## Docker Note

Inside Docker all services share the `blinkit-network` bridge network. The Eureka server container is named `blinkit-eureka`, so every service connects using:

```yaml
EUREKA_SERVER_URL: http://eureka-server:8761/eureka
```

The container name `eureka-server` is Docker's internal DNS — it resolves to the correct container IP automatically. `localhost:8761` would NOT work inside Docker.

---

## Startup Order

Eureka must be healthy before any other service starts. The `docker-compose.yml` enforces this:

```yaml
auth-service:
  depends_on:
    eureka-server:
      condition: service_healthy   # waits until Eureka passes health check
```

All 12 application services have this dependency so they only start after Eureka is ready to accept registrations.

---

# InternalRequestFilter — Direct Port Access Protection

## What Problem Does It Solve?

Every microservice runs on its own port. Without protection, an adversary or misconfigured client can bypass the API Gateway entirely and hit a service directly:

```
# Correct path — through gateway (JWT validated, X-User-Id injected)
curl http://localhost:8080/api/users/profile -H "Authorization: Bearer <token>"

# Direct port hit — bypasses gateway entirely (security hole without filter)
curl http://localhost:8082/users/profile
→ Would reach the controller with no JWT validation, no X-User-Id
```

`InternalRequestFilter` closes this hole by requiring a shared secret header (`X-Internal-Secret`) that only the API Gateway knows and injects on every forwarded request.

---

## How It Works

```
Request arrives at downstream service port
         ↓
InternalRequestFilter runs (OncePerRequestFilter)
         ↓
Is path /actuator/** ?
   YES → pass through (Eureka health checks hit the port directly)
   NO  ↓
Header X-Internal-Secret present and correct?
   YES → pass through to controller
   NO  → 403 Forbidden
         { "success": false, "message": "Access denied. Use the API Gateway.", "data": null }
```

---

## Where the Secret Comes From

**`.env`:**
```
INTERNAL_SECRET=blinkit-internal-svc-secret-2026
```

**Shared config** (`config-server/configs/application.yml`) — fetched by all services on startup:
```yaml
internal:
  secret-key: ${INTERNAL_SECRET:blinkit-internal-svc-secret-2026}
```

**Filter reads it via:**
```java
@Value("${internal.secret-key}")
private String internalSecret;
```

---

## How the Gateway Injects the Secret

In `JwtAuthFilter.java`, the secret is injected on **every** forwarded request — both public and authenticated paths:

```java
// Public paths (no JWT) — secret still injected
exchange.mutate().request(r -> r.header("X-Internal-Secret", internalSecret)).build();

// Authenticated paths — secret injected alongside user context headers
exchange.mutate().request(r -> r
        .header("X-User-Id",        userId)
        .header("X-User-Role",      role)
        .header("X-User-Email",     email)
        .header("X-Internal-Secret", internalSecret))   // always present
        .build();
```

---

## Services With InternalRequestFilter

| Service | Port | Has Filter |
|---------|------|------------|
| auth-service | 8081 | ✅ |
| user-service | 8082 | ✅ |
| product-service | 8083 | ✅ |
| inventory-service | 8084 | ✅ |
| notification-service | 8089 | ✅ |
| eureka-server | 8761 | ❌ — infrastructure, services register directly |
| config-server | 8888 | ❌ — infrastructure, services fetch config on startup |
| api-gateway | 8080 | ❌ — this IS the gateway |

---

## Why OncePerRequestFilter

`InternalRequestFilter` extends `OncePerRequestFilter` instead of raw `jakarta.servlet.Filter`. This guarantees the filter executes **exactly once per request**, even when the servlet container internally re-dispatches the same request (e.g. async dispatch, forward, include). Without this guarantee, the filter could run multiple times and produce duplicate responses.

---

## Auth-Service Special Case

Auth-service has both Spring Security (`SecurityConfig`) and `InternalRequestFilter`. They run in this order:

```
Request arrives at port 8081
         ↓
Spring Security filter chain (order: -100)
   → /auth/** and /actuator/** are permitAll() — passes through
         ↓
InternalRequestFilter (order: 0, default for @Component)
   → Checks X-Internal-Secret
         ↓
Controller
```

No conflict — Spring Security handles authentication rules, `InternalRequestFilter` handles gateway-origin enforcement. Both must pass for a request to reach the controller.

---

## Full Security Flow With Filter in Place

```
Internet
   ↓
API Gateway :8080
   → Validates JWT (signature + expiry + Redis blacklist)
   → Injects X-User-Id, X-User-Role, X-User-Email
   → Injects X-Internal-Secret
   ↓
Downstream Service :820x
   → InternalRequestFilter checks X-Internal-Secret → 403 if missing/wrong
   → Controller reads X-User-Id (trusts gateway already validated JWT)
```

Direct access to any service port without the correct secret → **always 403**, even with a valid JWT token.
