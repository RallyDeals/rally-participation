# Participation Service

Rally / GroupDeal — Java 21 / Spring Boot 3.3, Maven.

Implements the design in `participation-service-docs.md`: join/leave, participant
listing, deal progress, and referral links, backed by Postgres + Kafka with a
transactional outbox for reliable event publication.

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker (for local Postgres + Kafka)

This project was scaffolded in a sandbox with no network access to Maven Central, so
it hasn't been compiled here — run `mvn compile` locally first thing to catch anything
version-related (Spring Boot 3.3.4 / spring-kafka's transitive Kafka client version
occasionally need a bump depending on what's current when you pull dependencies).

## Running locally

```bash
docker compose up -d          # Postgres on 5433, Kafka (KRaft) on 9092
mvn spring-boot:run
```

The app runs on port **8086**. Flyway applies `V1__init.sql` automatically on startup
(the schema from the docs, plus `participation_outbox`).

Kafka topics (`participant.joined`, `participant.left`,
`order.deal_order_cancelled`) aren't auto-created by the compose file — either enable
Kafka's `auto.create.topics.enable` (on by default in the Confluent image used here) or
create them explicitly if you disable that.

## Auth

Every request must carry an `X-User-Id: <uuid>` header — this stands in for what the
API Gateway is expected to set after validating the caller's JWT (per your answer, this
service doesn't validate JWTs itself). There's no gateway in this repo, so for local
testing just set the header by hand, e.g.:

```bash
curl -X POST http://localhost:8083/deals/<dealId>/join \
  -H "X-User-Id: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{}'
```

## Deal Service integration

`DealServiceClient` has two implementations, switched via Spring profile
(`spring.profiles.active` in `application.yml`, default `stub-deal-service`):

- **`StubDealServiceClient`** — in-memory, capacity-limited per deal (default 5, see
  `rally.deal-service.stub.default-capacity`) so 409s are exercisable end to end without
  a real Deal Service running. Fabricates `getDealSummary()` responses too
  (`default-min-participants`, `default-end-time-days-from-now`).
- **`RealDealServiceClient`** — WebClient-backed, calls `POST /deals/{id}/reserve-slot`,
  `POST /deals/{id}/check-leave-eligible`, and `GET /deals/{id}/summary` against
  `rally.deal-service.base-url`. Short timeout (`rally.deal-service.timeout-ms`, default
  2s) plus a small bounded retry with backoff for transient failures only — 404/409 are
  never retried, and any failure that isn't a clean 404/409 becomes a `503` rather than
  being treated as success.

Switch once Deal Service exists:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=real-deal-service
# or: SPRING_PROFILES_ACTIVE=real-deal-service mvn spring-boot:run
```

Confirm the actual response shape of `GET /deals/{id}/summary` against Deal Service once
it's built — `DealSummaryResponse` is currently just Participation Service's assumption
of what it needs.

## Tests

```bash
mvn test                                    # unit tests only
mvn verify -Dit.test=*IT -DskipITs=false    # if you wire a failsafe profile, or just:
mvn test -Dtest=*IT                         # run the IT classes directly with surefire
```

Unit tests (fast, no Docker) cover the service layer (join/leave happy paths, referral
resolution, capacity/eligibility rejections, the unique-index race → 409 translation,
progress-response assembly), the stub Deal Service client, and `RealDealServiceClient`
against a MockWebServer (success, 404, 409, retry-exhausted-on-5xx).

Integration tests (`src/test/java/.../integration/*IT.java`) need **Docker** — they spin
up real Postgres and Kafka via Testcontainers:
- `ParticipationRepositoryIT` — verifies the partial unique index against real Postgres
  (H2's `MODE=PostgreSQL` doesn't faithfully emulate partial indexes, so this is worth
  having even though it's slower).
- `OutboxPollerIT` — writes an outbox row directly, runs the poller, and asserts the
  message actually lands on the real Kafka topic and the row gets marked published.
