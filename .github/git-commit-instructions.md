# Git Commit Instructions — FraudGuard

## Language rule

> All commit messages **must be written in Portuguese (pt-BR)**.
> These instructions are written in English.

---

## Commit message format

```
<type>(<scope>): <short description in Portuguese>

[optional body in Portuguese]

[optional footer(s)]
```

- **Header line:** 72 characters max.
- **Body:** wrap at 72 characters; explain *what* and *why*, not *how*.
- **Blank line** between header, body, and footer.

---

## Types

| Type       | When to use                                             |
|------------|---------------------------------------------------------|
| `feat`     | New feature or capability                               |
| `fix`      | Bug fix                                                 |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test`     | Adding or correcting tests                              |
| `chore`    | Build, tooling, dependency updates, CI/CD config        |
| `docs`     | Documentation only                                      |
| `perf`     | Performance improvement                                 |
| `style`    | Formatting, missing semicolons — no logic change        |
| `revert`   | Reverts a previous commit                               |

---

## Scopes

Use the **service or cross-cutting concern** affected:

| Scope           | Maps to                                                        |
|-----------------|----------------------------------------------------------------|
| `account`       | account-service                                                |
| `transaction`   | transaction-service                                            |
| `fraud`         | fraud-engine                                                   |
| `notification`  | notification-service                                           |
| `infra`         | Docker Compose, Kubernetes manifests                           |
| `kafka`         | Kafka topics, producers, consumers, DLQ                        |
| `redis`         | Redis keys, cache strategy, pipelines                          |
| `db`            | PostgreSQL schema, migrations, indexes                         |
| `mongo`         | MongoDB collections, audit documents                           |
| `observability` | Micrometer metrics, Prometheus, Grafana, Zipkin, OpenTelemetry |
| `security`      | Keycloak integration, X-User-Id header, JWT config             |
| `ci`            | GitHub Actions workflows                                       |
| `aws`           | ECS, ECR, SQS, IAM, Parameter Store                            |
| `config`        | application.yaml, environment properties                       |
| `deps`          | Dependency version updates (pom.xml)                           |

---

## Rules

1. **Present indicative in Portuguese** — conjugate the verb in the third-person singular present tense (ele/ela form). Reference: [Conventional Commits pt-BR](https://www.conventionalcommits.org/pt-br/v1.0.0-beta.4/).
   - ✅ `feat(account): adiciona endpoint de atualização de perfil`
   - ✅ `feat(account): implementa validação de CPF no cadastro`
   - ❌ `feat(account): adicionar endpoint de atualização de perfil` (infinitive — avoid)
   - ❌ `feat(account): adicionado endpoint de atualização de perfil` (past tense — avoid)

2. **No period at the end** of the header line.

3. **Lowercase** header (except proper nouns like Redis, Kafka, Keycloak, AWS).

4. **One logical change per commit** — do not bundle unrelated changes.

5. **Reference requirement IDs** when applicable (RF-01, RNF-02, etc.).

6. **Breaking changes** must have a `BREAKING CHANGE:` footer **and** a `!` after the type/scope.
   ```
   refactor(fraud)!: remove suporte ao modo síncrono de análise

   BREAKING CHANGE: o fraud-engine agora opera exclusivamente via Kafka.
   Remova qualquer chamada REST direta ao fraud-engine.
   ```

7. **Never commit** secrets, hardcoded credentials, passwords, or JWT tokens.

8. **Never commit** commented-out code blocks unless there is an explanatory note.

---

## Examples by service

### account-service
```
feat(account): implementa RF-01 — cadastro de usuário com validação de CPF

Adiciona endpoint POST /accounts com validação de CPF único e e-mail único.
Encaminha criação do usuário ao Keycloak via RestClient.
Publica evento AccountCreated no tópico account.created.

Refs: RF-01, RNF-04
```

```
feat(account): adiciona cache Redis no perfil do usuário (RNF-01)

Chave: account:profile:{userId}, TTL: 30min.
Usa @Cacheable na consulta e @CacheEvict na atualização.
```

```
fix(account): corrige eviction do cache ao atualizar e-mail

O @CacheEvict não estava sendo disparado quando apenas o e-mail era alterado.
```

### transaction-service
```
feat(transaction): implementa idempotência via Redis SETNX (RNF-01)

Chave: idem:{idempotencyKey}, TTL: 30min.
Retorna 200 com resposta original se a chave já existir.
```

```
feat(transaction): implementa Outbox Pattern para publicação no Kafka (RNF-02)

Persiste transação e evento outbox na mesma transação de banco.
Scheduler publica eventos PENDING a cada 5 segundos.
```

```
feat(transaction): consome FraudDecision e atualiza status da transação (RF-05)

BLOCK → REJECTED; ALLOW e FLAG → APPROVED.
FLAG gera alerta via log estruturado sem bloquear a transação.
```

### fraud-engine
```
feat(fraud): implementa FrequencyRule com Sorted Set no Redis

Detecta mais de N transações em janela deslizante de 60s.
Usa ZREMRANGEBYSCORE + ZCOUNT em pipeline único (RNF-01).
```

```
feat(fraud): publica FraudDecision no tópico fraud.decision (RF-03)

Payload: transactionId, userId, result, score, reasons, timestamp.
```

```
perf(fraud): consolida operações Redis da análise em pipeline único (RNF-01)
```

### notification-service
```
feat(notification): consome FraudDecision e emite notificação mock (RF-01, RF-02)

Notificação registrada como log estruturado JSON com userId e resultado.
```

### Infrastructure / Observability
```
chore(infra): adiciona Docker Compose com PostgreSQL, Redis, Kafka e Keycloak
```

```
chore(infra): inclui Prometheus, Grafana e Zipkin no Docker Compose
```

```
feat(observability): expõe métricas do account-service via Micrometer (RNF-02)

Counters: account.registrations.total (tags: status), account.profile.cache.hit.
```

```
chore(ci): adiciona workflow GitHub Actions com build, testes e JaCoCo
```

```
chore(aws): adiciona task definition ECS para account-service
```

### Dependencies / Config
```
chore(deps): atualiza spring-boot-starter-parent para 3.x.x
```

```
chore(config): externaliza configurações sensíveis para AWS Parameter Store
```

### Tests
```
test(account): adiciona testes de integração com Testcontainers para RF-01
```

```
test(fraud): cobre FrequencyRule com testes unitários usando Mockito
```

---

## What NOT to do

```
# Too vague
fix: correção

# English message
feat(account): add user registration endpoint

# Multiple unrelated concerns in one commit
feat(account): add registration + fix fraud pipeline + update docker-compose

# Infinitive — avoid
feat(account): adicionar endpoint de atualização de perfil

# Past tense — avoid
feat(transaction): adicionado suporte a idempotência
```

---

## Quick reference

```
feat(account): <descrição>        → adiciona funcionalidade no account-service
fix(fraud): <descrição>           → corrige bug no fraud-engine
refactor(transaction): <descrição>→ refatora código no transaction-service
test(notification): <descrição>   → adiciona teste no notification-service
chore(infra): <descrição>         → infra/docker/k8s
chore(ci): <descrição>            → GitHub Actions
chore(deps): <descrição>          → dependências Maven
feat(observability): <descrição>  → métricas/logs/traces
```

