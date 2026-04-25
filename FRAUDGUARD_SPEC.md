# Picpay Fraud Guard — Project Specification

> Arquivo de contexto para agente LLM (Claude Sonnet via GitHub Copilot).
> Referencie este arquivo no início de cada sessão de desenvolvimento.
> Não reexplique o contexto — leia este arquivo e prossiga diretamente para a tarefa solicitada.

---

## 1. Developer profile

- **Name:** Sidney
- **Role:** Senior Backend Developer — Java since 2018
- **Current stack (daily):** Java 17, Spring Boot, Kafka, Docker, Kubernetes, JUnit 5, Mockito, REST APIs, microservices
- **Goal:** Apply for Mid/Senior Back-End Java Developer at PicPay — Banking BU
- **Known gaps to address:** AWS (no hands-on), Kotlin (beginner), Observability stack, Redis (surface level), CI/CD pipeline

---

## 2. Project overview

**Name:** FraudGuard  
**Type:** Proof of Concept — real-time fraud analysis platform  
**Purpose:** Portfolio project aligned with PicPay Banking BU job requirements  
**Language mix:** Java (account-service, transaction-service) + Kotlin (fraud-engine, notification-service — introduced in Week 9)

### Job requirements this POC covers

| Requirement                     | How FraudGuard covers it                                  |
|---------------------------------|-----------------------------------------------------------|
| Java + Spring Boot              | All services                                              |
| Kafka (messaging)               | Inter-service event communication                         |
| Redis (cache + data structures) | Idempotency, fraud state, frequency sliding window        |
| SQL + NoSQL                     | PostgreSQL (transactional) + MongoDB (audit log)          |
| Microservices + REST            | 4 independent services with REST APIs                     |
| AWS + CI/CD                     | ECS deploy, SQS, ECR, GitHub Actions pipeline             |
| Observability                   | Micrometer + Prometheus + Grafana + OpenTelemetry         |
| SOLID + Design Patterns         | Strategy (fraud rules), Outbox, Chain of Responsibility   |
| Kotlin                          | fraud-engine and notification-service rewritten in Week 9 |
| Real-time fraud score           | fraud-engine core responsibility                          |

---

## 3. Architecture

### Entry point

```
External client
      ↓
Amazon API Gateway
  - Rate limiting
  - Load balancing
  - Delegates token validation to Keycloak
  - Injects X-User-Id header after successful validation
      ↓             ↑ validates JWT
   Keycloak (Identity Provider)
  - User registration + login
  - JWT issuance (OAuth2 / OIDC)
  - Token refresh and revocation
  - Runs as a container (local) or managed instance (prod)
      ↓
Internal services (trusted network)
```

> Internal services never validate JWT. They trust `X-User-Id` header injected by the Gateway.  
> Keycloak is the sole authority for authentication and token lifecycle — no service issues tokens.

### Services

| Service              | Language      | Port | DB                   | Responsibility                                                              |
|----------------------|---------------|------|----------------------|-----------------------------------------------------------------------------|
| account-service      | Java          | 8080 | PostgreSQL           | User business data (profile, registration, update) — no auth responsibility |
| transaction-service  | Java          | 8081 | PostgreSQL + MongoDB | Transaction submission, idempotency, status tracking                        |
| fraud-engine         | Java → Kotlin | 8082 | MongoDB              | Real-time fraud rule pipeline, score computation                            |
| notification-service | Java → Kotlin | 8083 | —                    | Consumes FraudDecision, sends mock notification                             |

### Event flow

```
account-service     → Kafka → AccountCreated      → fraud-engine (init risk profile)
transaction-service → Kafka → TransactionSubmitted → fraud-engine (analyze)
fraud-engine        → Kafka → FraudDecision        → transaction-service (update status)
fraud-engine        → Kafka → FraudDecision        → notification-service (notify)
```

### Kafka topics

| Topic                   | Producer            | Consumer(s)                               |
|-------------------------|---------------------|-------------------------------------------|
| `account.created`       | account-service     | fraud-engine                              |
| `transaction.submitted` | transaction-service | fraud-engine                              |
| `fraud.decision`        | fraud-engine        | transaction-service, notification-service |

---

## 4. Infrastructure (Docker Compose — local dev)

```yaml
services:
  postgres:        # shared between account-service and transaction-service (separate schemas)
  mongodb:         # fraud-engine audit log + transaction-service event store
  redis:           # maxmemory 256mb, policy allkeys-lru — spring-boot-starter-data-redis only
  kafka:           # with Zookeeper or KRaft
  zookeeper:
  keycloak:        # identity provider — JWT issuance, OAuth2/OIDC, port 8180
  prometheus:      # scrapes /actuator/prometheus every 15s
  grafana:         # dashboards, port 3000
  zipkin:          # distributed tracing, port 9411
```

> Localstack will be added to simulate AWS services (SQS, ECS, Parameter Store) locally.

---

## 5. Service specifications

### 5.1 account-service

**Functional requirements:**

| ID    | Requirement            | Rule                                                                    |
|-------|------------------------|-------------------------------------------------------------------------|
| RF-01 | User registration      | CPF (unique, validated), email (unique), password forwarded to Keycloak |
| RF-02 | Profile query          | userId from `X-User-Id` header — never from token                       |
| RF-03 | Publish AccountCreated | Payload: userId, email, createdAt, accountType                          |
| RF-04 | Update profile         | Name and email only — CPF is immutable                                  |

> Authentication (login, JWT issuance, token refresh) is fully delegated to Keycloak via API Gateway.  
> account-service has no auth endpoint and no knowledge of JWT internals.

**Non-functional requirements:**

| ID     | Requirement         | Detail                                                            |
|--------|---------------------|-------------------------------------------------------------------|
| RNF-01 | Redis profile cache | Key: `account:profile:{userId}`, TTL: 30min, evict on update      |
| RNF-02 | Micrometer metrics  | Registrations/min, duplicate CPF rate, profile cache hit rate     |
| RNF-03 | Structured logs     | JSON with MDC: userId, traceId                                    |
| RNF-04 | PostgreSQL          | Unique index on CPF and email — password never stored or returned |
| RNF-05 | Health check        | `/actuator/health` — checks DB and Redis                          |

**REST endpoints:**

```
POST   /accounts      → RF-01 register (triggers Keycloak user creation)
GET    /accounts/me   → RF-02 profile (header: X-User-Id)
PUT    /accounts/me   → RF-04 update profile
```

---

### 5.2 transaction-service

**Functional requirements:**

| ID    | Requirement                  | Rule                                                                      |
|-------|------------------------------|---------------------------------------------------------------------------|
| RF-01 | Submit transaction           | userId from `X-User-Id` header; body: amount, merchantId, idempotencyKey  |
| RF-02 | Idempotency                  | Header: `X-Idempotency-Key`; Redis SETNX; key: `idem:{key}`, TTL: 30min   |
| RF-03 | Publish TransactionSubmitted | Payload: transactionId, userId, merchantId, amount, timestamp, region     |
| RF-04 | Query transaction status     | Returns: PENDING / APPROVED / REJECTED + reason                           |
| RF-05 | Consume FraudDecision        | BLOCK → REJECTED; ALLOW/FLAG → APPROVED (FLAG generates alert, not block) |

**Non-functional requirements:**

| ID     | Requirement           | Detail                                                                                |
|--------|-----------------------|---------------------------------------------------------------------------------------|
| RNF-01 | Idempotency via Redis | SETNX atomic — safe under concurrent retries                                          |
| RNF-02 | Outbox Pattern        | Transaction + outbox event saved in same DB transaction; scheduler publishes to Kafka |
| RNF-03 | Business metrics      | TPS, average amount, rejection rate, submission latency — tags: status, region        |
| RNF-04 | Circuit Breaker       | Resilience4j — Redis timeout: 200ms, DB timeout: 500ms, opens after 5 failures        |
| RNF-05 | PostgreSQL indexes    | Composite index on (userId, status, createdAt)                                        |

**REST endpoints:**

```
POST   /transactions         → RF-01 submit transaction
GET    /transactions/{id}    → RF-04 query status
```

**Outbox table schema:**

```sql
CREATE TABLE outbox (
  id           UUID PRIMARY KEY,
  topic        VARCHAR(100) NOT NULL,
  payload      JSONB NOT NULL,
  status       VARCHAR(10) DEFAULT 'PENDING',  -- PENDING | SENT
  created_at   TIMESTAMP NOT NULL
);
```

---

### 5.3 fraud-engine

**Functional requirements:**

| ID    | Requirement                  | Rule                                                               |
|-------|------------------------------|--------------------------------------------------------------------|
| RF-01 | Consume TransactionSubmitted | Via Kafka — trigger fraud analysis pipeline                        |
| RF-02 | Run fraud rules              | Strategy pattern — each rule returns ALLOW / FLAG / BLOCK + reason |
| RF-03 | Publish FraudDecision        | Payload: transactionId, userId, result, score, reasons, timestamp  |
| RF-04 | Initialize user risk profile | Triggered by AccountCreated — creates Hash in Redis                |

**Fraud rules (Strategy pattern):**

| Rule               | Redis structure                      | Logic                                                |
|--------------------|--------------------------------------|------------------------------------------------------|
| FrequencyRule      | Sorted Set `fraud:freq:{userId}`     | > N transactions in 60s window                       |
| VelocityAmountRule | Sorted Set `fraud:velocity:{userId}` | > R$ threshold in window                             |
| GeoVelocityRule    | Sorted Set `fraud:geo:{userId}`      | Geographically impossible speed between transactions |
| MultiDeviceRule    | Set `user:devices:{userId}`          | > 3 distinct devices                                 |
| MerchantRiskRule   | Sorted Set `fraud:merchant:risk`     | Merchant score above threshold                       |
| BurstRule          | Sorted Set `fraud:freq:{userId}`     | Multi-window burst detection (10s, 1min, 5min, 1h)   |

**Non-functional requirements:**

| ID     | Requirement    | Detail                                                                    |
|--------|----------------|---------------------------------------------------------------------------|
| RNF-01 | Redis pipeline | All Redis ops per analysis in single roundtrip                            |
| RNF-02 | MongoDB audit  | Every decision persisted as immutable document                            |
| RNF-03 | Custom metrics | `fraud.analysis.duration` (p50/p95/p99), `fraud.decision.total` by result |
| RNF-04 | DLQ            | Failed messages after 3 retries → dead-letter topic                       |

---

### 5.4 notification-service

**Functional requirements:**

| ID    | Requirement                             |
|-------|-----------------------------------------|
| RF-01 | Consume FraudDecision from Kafka        |
| RF-02 | Send mock notification (structured log) |

**Non-functional requirements:**

| ID     | Requirement                    | Detail                                          |
|--------|--------------------------------|-------------------------------------------------|
| RNF-01 | Retry with exponential backoff | Resilience4j — 3 retries, backoff: 1s / 2s / 4s |
| RNF-02 | Dead-letter queue              | After all retries fail → DLQ topic              |

---

## 6. Redis usage map

| Key pattern                | Structure        | Service             | Purpose                        |
|----------------------------|------------------|---------------------|--------------------------------|
| `account:profile:{userId}` | String (JSON)    | account-service     | Profile cache, TTL 30min       |
| `idem:{idempotencyKey}`    | String           | transaction-service | Idempotency, TTL 30min         |
| `fraud:freq:{userId}`      | Sorted Set       | fraud-engine        | Frequency sliding window (60s) |
| `fraud:velocity:{userId}`  | Sorted Set       | fraud-engine        | Amount velocity window         |
| `fraud:geo:{userId}`       | Sorted Set       | fraud-engine        | Geolocation history            |
| `risk:profile:{userId}`    | Hash             | fraud-engine        | Risk score, avg ticket, region |
| `user:devices:{userId}`    | Set              | fraud-engine        | Unique device fingerprints     |
| `fraud:merchant:risk`      | Sorted Set       | fraud-engine        | Merchant risk ranking          |
| `rate:{userId}`            | String (counter) | transaction-service | Rate limiting (INCR + EXPIRE)  |

---

## 7. Observability

### Metrics (Micrometer → Prometheus → Grafana)

```
# account-service
account.registrations.total        counter   tag: status (success|duplicate_cpf)
account.profile.cache.hit          counter   tag: result (hit|miss)

# transaction-service
transaction.submitted.total        counter   tags: status, region
transaction.submission.duration    timer     p50/p95/p99
transaction.amount.summary         distsum   value distribution

# fraud-engine
fraud.analysis.duration            timer     p50/p95/p99
fraud.decision.total               counter   tags: result (ALLOW|FLAG|BLOCK), reason
fraud.analysis.active              gauge     in-flight analyses
```

### Logging

- Format: JSON (Logstash encoder)
- MDC fields: `traceId`, `spanId`, `userId`, `txId`
- Level: INFO for normal flow, ERROR with full stack trace on failures

### Tracing

- Library: OpenTelemetry + Micrometer Tracing
- Exporter: Zipkin (local), Dynatrace (production differentiator)
- Sampling: 100% (dev) / 10% (prod)

---

## 8. CI/CD pipeline (GitHub Actions)

```
push to main
    ↓
build (mvn package -DskipTests)
    ↓
unit + integration tests (Testcontainers)
    ↓
code coverage (JaCoCo — minimum 80%)
    ↓
build Docker image
    ↓
push to ECR
    ↓
deploy to ECS (Fargate)
```

---

## 9. Study plan progress

| Phase | Weeks | Topics                                             | Status                    |
|-------|-------|----------------------------------------------------|---------------------------|
| 1     | 1–2   | Redis, AWS, Observability                          | **DONE — see section 10** |
| 2     | 3–5   | SOLID, Design Patterns, Testcontainers, MongoDB    | Pending                   |
| 3     | 6–8   | Advanced Kafka, Saga, Resilience4j, SQL tuning     | Pending                   |
| 4     | 9–11  | Kotlin, CI/CD pipeline, Advanced Kubernetes        | Pending                   |
| 5     | 12    | Portfolio polish, C4 diagram, ADRs, interview prep | Pending                   |

---

## 10. Topics studied (Week 1–2 — reference only)

### Redis
- Cache patterns: cache-aside, write-through, write-behind
- Spring annotations: `@Cacheable`, `@CacheEvict`, `@CachePut`
- `RedisTemplate` vs annotations — when to use each
- Eviction policies: allkeys-lru (default for FraudGuard), volatile-lru, noeviction
- Data structures: String, Hash, Sorted Set, Set, List — use case per structure
- Sorted Set: sliding window pattern (timestamp as score, ZREMRANGEBYSCORE + ZCOUNT)
- Pipeline: multiple commands in single network roundtrip

### AWS
- ECS (Fargate): Cluster → Service → Task Definition → Task hierarchy
- SQS: Standard vs FIFO — FIFO for financial transactions (exactly-once, ordered)
- IAM: Roles + Policies, least privilege principle, containers assume roles (no hardcoded keys)
- Parameter Store: externalized config read by Spring Boot at startup
- ECR: private image registry — bridge between CI/CD and ECS
- CloudWatch Logs: automatic log destination for ECS containers

### Observability
- Three pillars: Metrics (what), Logs (exactly what), Traces (where in time)
- Micrometer types: Counter, Timer, Gauge, DistributionSummary
- Stack: Micrometer → Prometheus (scrape) → Grafana (dashboards)
- Structured logs: JSON + Logstash encoder + MDC for correlation
- Distributed tracing: OpenTelemetry + Zipkin, traceId auto-propagated

---

## 11. Next immediate task

**Implement account-service and transaction-service (Weeks 1–2 deliverable)**

Start with:
1. Package structure for both services
2. Core classes: entities, repositories, services, controllers, events
3. Docker Compose base configuration (PostgreSQL, Redis, Kafka, Zookeeper, Prometheus, Grafana, Zipkin)
4. account-service: RF-01, RF-02, RF-03 + RNF-01, RNF-02, RNF-05
5. transaction-service: RF-01, RF-02, RF-03, RF-04, RF-05 + RNF-01, RNF-02, RNF-03

---

## 12. Coding conventions

- **Language:** Java 17 (services 1–2), Kotlin (services 3–4 from Week 9)
- **Framework:** Spring Boot 3.x
- **Build tool:** Maven
- **Test:** JUnit 5 + Mockito (unit) + Testcontainers (integration — from Week 3)
- **Logs:** SLF4J + Logback + Logstash encoder (JSON output)
- **API docs:** OpenAPI 3 via springdoc-openapi
- **Error handling:** `@ControllerAdvice` with RFC 7807 (Problem Details)
- **Package structure:** `com.fraudguard.{service-name}` → layers: domain / application / infrastructure / api
- **Redis dependency:** `spring-boot-starter-data-redis` only — never `spring-session-data-redis` (session-based, not applicable to JWT/REST)
- **Never:** hardcode credentials, return passwords in responses, validate JWT inside services, issue tokens outside Keycloak

---

*Last updated: Session 1 — Weeks 1–2 complete. Corrections: Keycloak added as Identity Provider, JWT issuance removed from account-service, Redis dependency clarified.*
