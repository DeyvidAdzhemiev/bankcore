# BankCore

A production-grade banking platform built to demonstrate real-world Java and Spring Boot engineering.
This is not a tutorial project — it is designed to reflect the architecture, patterns, and decisions
you would encounter in a professional fintech system.

## What this project covers

Every component exists because it solves a real problem, not to check a box.

**Core Java & Spring**
Spring Boot 3.5, Spring Data JPA, Hibernate, Spring Security, Spring WebFlux, Spring AI,
bean scopes and lifecycle, `@Transactional` with isolation levels, N+1 query detection,
Flyway migrations, HikariCP connection pooling, Java 25 virtual threads.

**Concurrency & data integrity**
Optimistic locking (`@Version`), pessimistic locking (`SELECT FOR UPDATE`), ACID transactions,
isolation levels (`READ_COMMITTED`, `REPEATABLE_READ`, `SERIALIZABLE`), idempotency keys,
the outbox pattern, distributed locking with Redis.

**Kafka (in depth)**
Topics, partitions, consumer groups, offset management, manual commits, exactly-once semantics,
Avro schemas with schema registry, dead letter queues, the outbox pattern, consumer lag monitoring.

**Caching**
In-memory caching (`@Cacheable`), Redis distributed cache, cache-aside pattern,
cache invalidation strategies, TTL configuration, Spring Cache abstraction.

**Design & architecture**
Microservices with DB-per-service, CQRS, Saga pattern for distributed transactions,
Strategy, Builder, Factory, Observer, Repository patterns, SOLID, DRY, KISS,
RESTful API design with versioning and RFC 7807 error responses.

**Infrastructure**
Docker multi-stage builds, Kubernetes (deployments, services, config maps, secrets, health probes,
HPA), GitHub Actions CI/CD, AWS EKS + RDS deployment.

**Testing**
Unit tests (JUnit 5 + Mockito), integration tests (Testcontainers), `@DataJpaTest`, `@WebMvcTest`,
Spring Cloud Contract for API contracts, performance testing with k6.

---

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │  Rate limiting, JWT validation, routing
                        └────────┬────────┘
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │ User service │  │Auth service  │  │Account svc   │
      └──────────────┘  └──────────────┘  └──────┬───────┘
                                                  │
      ┌──────────────┐  ┌──────────────┐  ┌──────▼───────┐
      │ Card service │  │Fraud service │  │Transaction   │
      └──────────────┘  └──────────────┘  │   service    │
                                          └──────┬───────┘
                        ┌─────────────────────────▼──────┐
                        │         Kafka event bus         │
                        └──────┬──────────┬──────┬───────┘
                               ▼          ▼      ▼
                        ┌──────────┐ ┌────────┐ ┌──────────┐
                        │Audit svc │ │Notif.  │ │AI insights│
                        └──────────┘ └────────┘ └──────────┘
```

Each service owns its database. No shared tables. No cross-service joins.
Inter-service communication is either synchronous REST (query) or asynchronous Kafka (command/event).

### Services

| Service | Responsibility | Key concepts |
|---|---|---|
| `api-gateway` | Routing, rate limiting, JWT validation | Spring Cloud Gateway, Redis rate limiter |
| `auth-service` | Authentication, token issuance | Spring Security, JWT, OAuth2, BCrypt |
| `user-service` | User registration, profile management | REST, validation, bean lifecycle |
| `account-service` | Accounts, balances, locking | ACID, optimistic/pessimistic locking, isolation levels |
| `transaction-service` | Transfers, payment processing | Kafka, outbox pattern, idempotency, Saga |
| `card-service` | Virtual/physical cards, limits | JPA, domain events |
| `fraud-service` | Real-time fraud scoring | Spring AI, virtual threads, Strategy pattern |
| `notification-service` | Email, SMS, push notifications | Kafka consumer, template engine |
| `audit-service` | Immutable event log | Kafka consumer, append-only writes, jsonb |
| `bankcore-common` | Shared library (events, DTOs, utils) | Maven module, Money value object |

---

## Getting started

### Prerequisites

- Java 25
- Docker + Docker Compose
- Maven 3.9+

### Run infrastructure locally

```bash
git clone https://github.com/your-username/bankcore.git
cd bankcore
docker compose up -d
```

This starts: PostgreSQL (one instance per service schema), Redis, Kafka, Zookeeper, Elasticsearch.

### Run a service

```bash
cd account-service
mvn spring-boot:run
```

Or run all services:

```bash
mvn spring-boot:run -pl account-service &
mvn spring-boot:run -pl transaction-service
```

### Run tests

```bash
# all tests across all modules
mvn test

# single service
mvn test -pl account-service

# integration tests only (requires Docker for Testcontainers)
mvn test -pl account-service -Dgroups=integration
```

---

## Domain model

```
USER ──< ACCOUNT ──< TRANSACTION >── FRAUD_SCORE
           │
           └──< CARD

TRANSACTION ──> AUDIT_LOG   (via Kafka)
USER        ──> NOTIFICATION (via Kafka)
```

### Key design decisions

**`ACCOUNT.version`** — optimistic locking field. JPA `@Version` increments on every update.
Concurrent modifications throw `OptimisticLockException`, handled with retry logic in the service layer.

**`TRANSACTION.idempotency_key`** — unique constraint. Retried requests return the original result
without re-executing the transfer. Essential for safe client retries.

**`AUDIT_LOG.before_state / after_state`** — full entity state serialized as `jsonb` on every change.
Complete history without event sourcing complexity. Queryable and human-readable.

**DB-per-service** — each service has its own schema and datasource. Enforced by convention
and by the absence of any cross-service foreign keys in the codebase.

---

## Project structure

```
bankcore/
├── pom.xml                          # root POM, parent for all modules
├── docker-compose.yml               # local infrastructure
├── .github/workflows/               # CI/CD pipelines
│   ├── ci.yml                       # build + test on every PR
│   └── cd-*.yml                     # per-service deploy on merge to main
│
├── bankcore-common/                 # shared library (not a runnable service)
│   └── src/main/java/com/bankcore/common/
│       ├── event/                   # Kafka event DTOs
│       ├── dto/                     # shared request/response objects
│       ├── exception/               # base exceptions, ErrorResponse
│       └── util/                    # Money, IdGenerator, DateUtils
│
└── account-service/                 # example — all services follow this layout
    ├── Dockerfile
    ├── k8s/                         # deployment.yml, service.yml, configmap.yml
    └── src/
        ├── main/java/com/bankcore/account/
        │   ├── domain/              # entities, value objects
        │   ├── repository/          # JPA repositories + custom queries
        │   ├── service/             # business logic, @Transactional
        │   ├── api/                 # REST controllers, DTOs
        │   ├── kafka/               # producers and consumers
        │   ├── config/              # Spring beans, security, cache config
        │   └── exception/           # handlers, custom exceptions
        ├── main/resources/
        │   ├── application.yml
        │   └── db/migration/        # Flyway V1__, V2__...
        └── test/
            ├── unit/                # JUnit 5 + Mockito
            ├── integration/         # @SpringBootTest + Testcontainers
            └── contract/            # Spring Cloud Contract
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5 |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL |
| Messaging | Apache Kafka, Avro, Schema Registry |
| Caching | Redis, Spring Cache |
| Security | Spring Security, JWT, OAuth2 |
| AI | Spring AI |
| Search | Elasticsearch |
| Build | Maven 3.9 (multi-module) |
| Containers | Docker, Kubernetes |
| CI/CD | GitHub Actions |
| Cloud | AWS (EKS + RDS + S3) |
| Testing | JUnit 5, Mockito, Testcontainers, k6 |
| Migrations | Flyway |
| API docs | SpringDoc OpenAPI (Swagger UI) |

---

## CI/CD

Every push triggers the CI pipeline:

```
push → build → unit tests → integration tests → Docker build → push to ECR
                                                                     │
merge to main ──────────────────────────────────────────────────────▶ deploy to EKS
```

Each service has its own CD workflow that only triggers when files in that service's directory change,
so merging a change to `fraud-service` does not redeploy `account-service`.

---

## API documentation

Swagger UI is available on each running service at `/swagger-ui.html`.

The API Gateway aggregates all service specs at `http://localhost:8080/swagger-ui.html`.

---

## Concepts index

Looking for a specific topic? Here is where to find it in the codebase.

| Topic | Location |
|---|---|
| `@Transactional` + isolation levels | `account-service/service/AccountService.java` |
| Optimistic locking (`@Version`) | `account-service/domain/Account.java` |
| Pessimistic locking | `account-service/repository/AccountRepository.java` |
| Kafka producer + outbox pattern | `transaction-service/kafka/TransactionEventProducer.java` |
| Kafka consumer + DLQ | `transaction-service/kafka/TransactionEventConsumer.java` |
| Idempotency key handling | `transaction-service/service/TransactionService.java` |
| Spring Security filter chain | `auth-service/config/SecurityConfig.java` |
| JWT generation + validation | `auth-service/service/JwtService.java` |
| Redis distributed cache | `account-service/config/CacheConfig.java` |
| Virtual threads (Java 25) | `fraud-service/config/ThreadConfig.java` |
| Strategy pattern (fraud rules) | `fraud-service/rules/FraudRuleEngine.java` |
| Immutable audit log | `audit-service/service/AuditEventHandler.java` |
| Money value object | `bankcore-common/util/Money.java` |
| Testcontainers integration test | `account-service/test/integration/AccountServiceIT.java` |
| Kubernetes manifests | `account-service/k8s/` |
| GitHub Actions pipeline | `.github/workflows/ci.yml` |

---

## Status

| Service | Status |
|---|---|
| bankcore-common | 🟢 Done |
| account-service | 🟡 In progress |
| auth-service | ⚪ Planned |
| transaction-service | ⚪ Planned |
| card-service | ⚪ Planned |
| fraud-service | ⚪ Planned |
| notification-service | ⚪ Planned |
| audit-service | ⚪ Planned |
| api-gateway | ⚪ Planned |

---

## Author

Deyvid Adzhemiev — Senior Backend Engineer
[LinkedIn](https://www.linkedin.com/in/deyvid-adzhemiev-2913151a2/) ·
