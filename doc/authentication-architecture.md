# Design Spring Boot auth plan

Write isn't available as a deferred tool, and plan mode restricts me to writing only the plan file. Since I don't have Write access, I'll deliver the plan directly as my response — the parent agent reads assistant output. Here is the design.

---

# User-Service Authentication Architecture (Greenfield Design)

Target location: `doc/authentication-architecture.md`. Scope: Spring Boot 4.1 / Java 21 / JJWT 0.12.6 / Spring Security 6.x, no gateway, 8-service marketplace.

## 1. Token Strategy

**Decision: Access JWT + opaque-ish Refresh JWT, both signed RS256.**

- **Algorithm: RS256** (2048-bit RSA). HS256 requires the symmetric secret to live on *every* validating service — with 7 downstream services and no gateway, that surface is unacceptable. RS256 lets user-service hold the private key and everyone else pull the public key over JWKS. ES256 (ECDSA P-256) is a valid alternative (smaller tokens, faster verify) but Java's `SecureRandom` + JCA edge cases on ECDSA nonce reuse historically bit teams; RS256 is boringly safe and JJWT-native.
- **Access token TTL: 15 minutes.** Short enough that revocation lag is bounded, long enough to avoid refresh chatter.
- **Refresh token TTL: 14 days (sliding via rotation).** Delivered as a JWT so we get integrity/audience checks for free, but *authority* comes from the `user_sessions` row it references — the JWT is a signed pointer.
- **Claims (access):** `iss=https://user-service.marketplace.internal`, `aud=["marketplace-services"]`, `sub=<user_uuid>`, `role` (single: BUYER/MERCHANT/ADMIN), `sid=<session_uuid>`, `jti`, `iat`, `exp`, `email_verified`, `token_type=access`.
- **Claims (refresh):** same envelope, `token_type=refresh`, `aud=["user-service"]` (only we accept it), no `role` (must be re-read from DB on refresh so role changes propagate within ~15 min).
- **Why session_id in access token:** enables O(1) revocation check when downstream services opt into a Redis "revoked_sids" bloom set. Default path stays stateless.

## 2. Key Management

**Decision: Keys generated at bootstrap, persisted encrypted in Postgres `signing_keys` table, rotated every 90 days with 30-day overlap.**

- Table: `signing_keys(kid PK, algo, public_pem, private_pem_encrypted, status ENUM(ACTIVE, NEXT, RETIRED), created_at, activated_at, retired_at)`. Private key encrypted at rest via `KMS_MASTER_KEY` env var (AES-GCM wrap) — swap for AWS KMS/Vault later without schema change.
- **Signing**: always use the single `ACTIVE` key. **Rotation**: promote `NEXT` -> `ACTIVE`, old `ACTIVE` -> `RETIRED` but still published on JWKS until all issued tokens expire (retention = access TTL + refresh TTL + 1 day).
- **JWKS endpoint**: `GET /.well-known/jwks.json` — public, cacheable, returns all non-retired keys. `Cache-Control: max-age=300`. Each JWT header carries `kid` so verifiers pick the right key.
- Other services use Spring Security's `NimbusJwtDecoder.withJwkSetUri(...)` — it caches and refreshes automatically on unknown `kid`.

## 3. Refresh Token Model

**Decision: Server-side session table, rotation-on-use, reuse detection kills the whole family.**

- Every successful login inserts a `user_sessions` row and issues refresh JWT with `sid`.
- On `/auth/refresh`: verify JWT sig -> load session by `sid` -> check `refresh_token_hash` matches SHA-256 of the presented refresh token -> if match, issue new pair, update row (new hash, `last_used_at`, `rotation_count++`). If **hash mismatch but sid exists** -> reuse attack: revoke *all* sessions for that user, emit `SessionReuseDetected` event, force re-login.
- Bind to device: store `user_agent_hash`, `ip_first_seen`, `device_fingerprint` (ua-parser + IP subnet). Mismatch on refresh -> soft signal (log, don't block by default); configurable to hard-block for ADMIN.
- **`user_sessions` fields**: `id (uuid) PK`, `user_id FK`, `refresh_token_hash CHAR(64)`, `user_agent`, `user_agent_hash`, `ip`, `device_label`, `created_at`, `last_used_at`, `expires_at`, `rotation_count INT`, `revoked_at NULL`, `revoked_reason ENUM`.

## 4. Password Storage

**Decision: BCrypt cost 12.**

- Argon2id is theoretically stronger (memory-hard), but Spring Security's `BCryptPasswordEncoder` is first-class, cost-12 gives ~250ms on modern hardware which is the sweet spot for a login endpoint, and BCrypt's 72-byte input limit is easy to guard. Argon2 via `Argon2PasswordEncoder` is available if we ever need it — `DelegatingPasswordEncoder` with `{bcrypt}` prefix leaves the door open for zero-downtime migration.
- Password policy: min 12 chars, zxcvbn score >= 3, checked against HIBP k-anonymity API on register/change.

## 5. Endpoints

All under `/api/v1/auth` unless noted. JSON in/out. Errors follow RFC 7807.

| Method | Path | Body | 200 Response |
|---|---|---|---|
| POST | `/auth/register` | `{email, password, role, displayName}` | `{userId, email, emailVerified:false}` |
| POST | `/auth/login` | `{email, password}` | `{accessToken, refreshToken, expiresIn}` (+ Set-Cookie if browser) |
| POST | `/auth/refresh` | `{refreshToken}` or cookie | same as login |
| POST | `/auth/logout` | refresh token | 204 |
| POST | `/auth/logout-all` | (bearer) | 204 |
| GET | `/users/me` | — | `{id, email, role, emailVerified, createdAt}` |
| GET | `/users/me/sessions` | — | `[{id, device, ip, createdAt, lastUsedAt, current:bool}]` |
| DELETE | `/users/me/sessions/{id}` | — | 204 |
| POST | `/auth/password/change` | `{currentPassword, newPassword}` | 204 (revokes other sessions) |
| POST | `/auth/password/reset/request` | `{email}` | 202 (always, no enumeration) |
| POST | `/auth/password/reset/confirm` | `{token, newPassword}` | 204 |
| GET | `/.well-known/jwks.json` | — | JWKS |
| GET | `/actuator/health` | — | Spring standard |

## 6. Token Transport

**Decision: Dual mode driven by `X-Client-Type` header.**

- **Browser SPAs (Merchant, Buyer):** access + refresh in **HttpOnly, Secure, SameSite=Lax** cookies (`__Host-access`, `__Host-refresh`). SameSite=Lax blocks cross-site POST CSRF for state-changing routes; combined with **double-submit CSRF token** in a readable `XSRF-TOKEN` cookie for defense-in-depth on mutating endpoints. Refresh cookie path scoped to `/api/v1/auth/refresh`.
- **Mobile / service-to-service:** `Authorization: Bearer <jwt>`. No CSRF concern (no ambient credentials).
- CORS: strict allowlist per SPA origin, `credentials: true`, no wildcard.

## 7. Multi-Provider Support

**Decision: Separate `user_oauth_identities` table from day one, even if only `LOCAL` provider ships in v1.**

- `users` table has **no** `password_hash` column directly — moved to `user_credentials(user_id, provider, credential_hash)` OR kept on `users` with `password_hash NULLABLE` (chosen: NULLABLE on users for simplicity; OAuth-only users have NULL).
- `user_oauth_identities(id, user_id FK, provider ENUM(GOOGLE, GITHUB, ...), provider_user_id, email_at_link, linked_at, UNIQUE(provider, provider_user_id))`.
- Account linking: if OAuth callback email matches existing verified local user -> link (require re-auth prompt). Otherwise create user.
- Provider abstraction: `AuthenticationProvider` Spring bean per source; `LocalAuthProvider` in v1, `OAuth2Provider` slots in via `spring-boot-starter-oauth2-client` later without touching the token issuance path.

## 8. Cross-Service Token Validation

**Decision: Local verification with cached JWKS. No `/auth/introspect` endpoint in v1.**

- Rationale: 7 services x every request through `/introspect` = user-service becomes the bottleneck and SPOF for the whole marketplace. With no gateway to fan out, local verify is the only scalable path.
- Each downstream service embeds a lightweight `marketplace-auth-lib` (5-file library: `JwtAuthFilter`, `JwksProvider`, `PrincipalMapper`, `RoleAnnotation`, autoconfig). Config: `marketplace.auth.jwks-uri=http://user-service/.well-known/jwks.json`, `marketplace.auth.issuer=...`, `marketplace.auth.audience=marketplace-services`.
- **Revocation gap**: up to `access TTL` (15 min). Acceptable. For sensitive ops (ADMIN destructive, payment authorize) services MAY additionally check a Redis `revoked_sid:<sid>` key populated by user-service on logout — opt-in per service.
- Nimbus decoder caches JWKS 5 min; user-service publishes `Cache-Control: max-age=300`.

## 9. Brute-Force / Lockout

**Decision: Redis-backed sliding-window counter + exponential backoff, no hard account lock.**

- Key: `login_fail:{email_hash}` and `login_fail_ip:{ip}`. Increment on failure, TTL 15 min.
- Thresholds: 5 failures/15min -> 1s delay, 10 -> 5s, 20 -> 30s, 50 -> soft-lock 1h (return generic 401, do not reveal lock). Never permanent lock (denial-of-service vector via known email).
- Library: **Bucket4j** (token-bucket) fronting `/auth/login`, `/auth/password/reset/request`, `/auth/refresh` at the HTTP layer, with Redis backing for distributed enforcement.

## 10. Event Publishing

**Decision: Outbox pattern -> message hub (Kafka/RabbitMQ TBD by infra). Same TX as the auth write.**

Events (topic: `user.events.v1`, JSON envelope `{eventId, eventType, occurredAt, userId, correlationId, payload}`):

- `UserRegistered` -> `{email, role, provider}`
- `LoginSucceeded` -> `{sessionId, ip, userAgent, provider}`
- `LoginFailed` -> `{email(hashed), ip, reason}` (Analytics + fraud detection)
- `PasswordChanged` -> `{}` (Notification sends confirmation email)
- `SessionRevoked` -> `{sessionId, reason}`
- `SessionReuseDetected` -> `{userId, sessionId, ip}` (SECURITY topic, higher priority)
- `EmailVerified`, `PasswordResetRequested` — hooks for Notification.

Outbox table `auth_event_outbox(id, aggregate_id, type, payload_json, created_at, published_at NULL)` polled by a `@Scheduled` publisher.

## 11. Data Model (DDL Sketch)

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email CITEXT UNIQUE NOT NULL,
  password_hash VARCHAR(72),               -- NULL for OAuth-only
  role VARCHAR(16) NOT NULL CHECK (role IN ('BUYER','MERCHANT','ADMIN')),
  display_name VARCHAR(120),
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/SUSPENDED/DELETED
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role_status ON users(role, status);

CREATE TABLE user_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  refresh_token_hash CHAR(64) NOT NULL,
  user_agent TEXT,
  user_agent_hash CHAR(64),
  ip INET,
  device_label VARCHAR(120),
  rotation_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  revoked_reason VARCHAR(32)
);
CREATE INDEX idx_sessions_user_active ON user_sessions(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_sessions_expires ON user_sessions(expires_at) WHERE revoked_at IS NULL;

CREATE TABLE user_oauth_identities (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider VARCHAR(32) NOT NULL,
  provider_user_id VARCHAR(255) NOT NULL,
  email_at_link CITEXT,
  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(provider, provider_user_id)
);
CREATE INDEX idx_oauth_user ON user_oauth_identities(user_id);

CREATE TABLE login_attempts (
  id BIGSERIAL PRIMARY KEY,
  email_hash CHAR(64),
  ip INET,
  succeeded BOOLEAN NOT NULL,
  failure_reason VARCHAR(32),
  attempted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attempts_email_time ON login_attempts(email_hash, attempted_at DESC);
CREATE INDEX idx_attempts_ip_time ON login_attempts(ip, attempted_at DESC);

CREATE TABLE signing_keys (...);           -- from Section 2
CREATE TABLE auth_event_outbox (...);      -- from Section 10
```

Partition `login_attempts` by month (retention 90 days).

## 12. Spring Security Wiring

```
SecurityFilterChain (order 1: /api/v1/auth/**)
  - sessionManagement STATELESS
  - csrf: enabled for cookie clients on mutating routes (CookieCsrfTokenRepository.withHttpOnlyFalse), disabled for Bearer
  - authorizeHttpRequests: /auth/register, /auth/login, /auth/refresh, /auth/password/reset/**, /.well-known/**, /actuator/health -> permitAll; rest -> authenticated
  - addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)
  - exceptionHandling: RFC7807 AuthenticationEntryPoint + AccessDeniedHandler
  - cors: strict per-origin
```

`JwtAuthenticationFilter`: reads Bearer OR `__Host-access` cookie, validates via `NimbusJwtDecoder` (own JWKS), maps claims -> `JwtAuthenticationToken` with `SimpleGrantedAuthority("ROLE_" + claim.role)`. Skips paths matching `permitAll` matcher.

Method security: `@EnableMethodSecurity`, use `@PreAuthorize("hasRole('MERCHANT')")`.

## 13. Extensibility Hooks

- **Email verification** (v1.1): `email_verification_tokens(token_hash, user_id, expires_at)`. `email_verified` claim already in JWT — services can gate behavior today.
- **TOTP / 2FA** (v1.2): `user_mfa(user_id, secret_encrypted, type, enabled_at, recovery_codes_hash[])`. Login endpoint returns `{mfaRequired:true, mfaToken}` short-lived challenge instead of tokens; `/auth/mfa/verify` completes it. `amr` claim in JWT lists factors.
- **Magic link** (v1.2): reuse email-verification token table with `purpose='LOGIN'`. No password path change required.
- **Step-up auth**: `acr` claim + `@PreAuthorize("hasAcr('urn:mkt:acr:mfa')")` custom expression — leave the seam.

## 14. Recommended Libraries Beyond Current Stack

| Library | Purpose | Priority |
|---|---|---|
| **Flyway** | Schema migrations — no migrations exists, mandatory before v1 | MUST |
| **Bucket4j** (+ `bucket4j-redis`) | Distributed rate limit on `/login`, `/refresh`, `/reset` | MUST |
| **Testcontainers** (postgresql, redis) | Integration tests without mocks | MUST |
| **Resilience4j** | Circuit breaker on outbound (KMS, email, HIBP) | SHOULD |
| **Nimbus JOSE+JWT** | Better JWKS handling than raw JJWT for downstream verifier lib | SHOULD |
| **MapStruct** | DTO <-> entity mapping (Lombok isn't enough) | SHOULD |
| **Spring Cloud Stream** or direct Kafka client | Event publishing to hub | MUST (once hub chosen) |
| **OWASP Java HTML Sanitizer** | display_name sanitization | SHOULD |
| **passay** or **zxcvbn4j** | Password strength policy | SHOULD |
| **Micrometer + Prometheus** | Auth metrics (login rate, failures, token issuance) | MUST |

## Cross-Cutting Decisions Summary

| Area | Choice | Killer reason |
|---|---|---|
| Algo | RS256 | No shared secrets across 7 services |
| Validation | Local + JWKS cache | user-service is not on the request hot path |
| Session store | Postgres row + JWT pointer | Revocable + stateless-ish |
| Rotation | On every refresh + reuse detection | Cheap, catches theft |
| Transport | Cookies for SPA, Bearer for mobile | CSRF vs XSS tradeoff picked per surface |
| Rate limit | Bucket4j + Redis | Distributed, per-endpoint |
| Migrations | Flyway from commit #1 | DIY schema doesn't scale past MVP |

---

### Critical Files for Implementation

- D:\Projects\marketplace\user-service\doc\authentication-architecture.md (the doc itself — target of this design)
- D:\Projects\marketplace\user-service\pom.xml (add Flyway, Bucket4j, Testcontainers, Nimbus, Spring Cloud Stream)
- D:\Projects\marketplace\user-service\src\main\resources\db\migration\V1__baseline_auth.sql (Flyway baseline covering all tables in Section 11)
- D:\Projects\marketplace\user-service\src\main\java\com\hvarakhobava\userservice\security\SecurityConfig.java (SecurityFilterChain from Section 12)
- D:\Projects\marketplace\user-service\src\main\java\com\hvarakhobava\userservice\security\jwt\JwtService.java (issue/verify + JWKS endpoint controller pair)
- D:\Projects\marketplace\user-service\doc\Marketplace HLA.drawio (source of truth for service topology — referenced by Section 8 rationale)