# Design

## Overview

This service receives order events from two external source systems with different JSON schemas, normalizes them into one internal canonical model, applies common business processing, and persists the transformed result to PostgreSQL.

The implementation intentionally keeps source-specific concerns separated from common processing so that new sources can be added without changing the shared business logic.

## Architecture

```text
Source A
   |
   v
orders.source-a.raw
   |
   v
SourceAOrderConsumer
   |
   v
SourceAOrderNormalizer
   |\
   | \
   |  \
   |   +----------------------+
   |                          |
   v                          v
CanonicalOrder         orders.normalized
                              ^
   ^                          |
   |                          |
   |   +----------------------+
   |  /
   | /
SourceBOrderNormalizer
   ^
   |
SourceBOrderConsumer
   ^
   |
orders.source-b.raw
   ^
   |
Source B

orders.normalized
      |
      v
NormalizedOrderConsumer (batch listener)
      |
      v
OrderProcessor
      |
      +--> CountryResolver
      |
      v
ProcessedOrder
      |
      v
TargetOrderSink
      |
      v
PostgresOrderSink (JDBC batch)
      |
      v
processed_orders
```

## Input boundary

Kafka is the defined input boundary for this service. The mechanism that reads from the original external systems and publishes into Kafka is assumed to be outside this service's ownership and scope.

No REST ingestion endpoint is implemented because the assignment only requires processing the source data, not owning the upstream transport into Kafka.

## Why Kafka

The expected load is about 50,000 orders/day, which is modest on average and does not by itself require Kafka. Kafka is still useful here because it provides:

- buffering between producers and processing
- replay after failure
- partition-based horizontal scaling
- failure isolation between stages
- independent scaling of source normalization and common processing
- a durable boundary between external source contracts and internal processing

The solution therefore uses Kafka for operational characteristics rather than because the raw throughput number demands it.

## Source-specific normalization

Each external schema has its own DTO and normalizer:

```java
public interface OrderNormalizer<T> {
    CanonicalOrder normalize(T order);
}
```

Implementations:

- `SourceAOrderNormalizer`
- `SourceBOrderNormalizer`

The canonical model contains only fields needed by common downstream processing:

```text
sourceSystem
orderReference
customerReference
customerFullName
countryCode
orderTimestamp
productCode
quantity
unitPrice
```

This isolates external schema changes. If Source B changes its nesting or field names, only the Source B DTO/normalizer should need modification.

## Canonical versus final model

`CanonicalOrder` is the shared internal model after source normalization.

`ProcessedOrder` matches the required final business shape and intentionally does not contain `sourceSystem`, because `sourceSystem` is not part of the assignment's target JSON.

Persistence still needs source identity for idempotency, so `TargetOrder` combines:

```text
sourceSystem + ProcessedOrder
```

This avoids polluting the required target model with persistence metadata.

## Country enrichment

Country reference data is abstracted behind:

```java
public interface CountryResolver {
    CountryInfo resolve(String countryCode);
}
```

The current implementation loads a small static map from `country-reference.yml`:

```text
US -> United States / USD
GB -> United Kingdom / GBP
DE -> Germany / EUR
```

A configuration-backed resolver was chosen because the reference set is tiny and supplied by the assignment.

A database, Redis, or remote reference-data service would add unnecessary operational complexity for the current requirement. The interface still makes replacement straightforward if the reference data later becomes dynamic.

## Business processing

`OrderProcessor` owns common validation, enrichment, and calculation.

Main transformation:

```text
totalOrderValue = quantity * unitPrice
```

`BigDecimal` is used for monetary values to avoid floating-point precision problems.

The processor validates required business fields, positive quantity, and non-negative unit price before producing `ProcessedOrder`.

## Target persistence assumption

The assignment specifies the target shape but not the target transport mechanism.

For this implementation, PostgreSQL is used as the target sink so that results are durable and easy to verify locally.

The persistence boundary is abstracted through `TargetOrderSink`, so another target adapter such as HTTP, Kafka, or a different database can replace PostgreSQL without changing normalization or business processing.

## PostgreSQL schema

The relational table stores explicit columns rather than one JSON/JSONB payload because the fields are known, stable, and useful for direct verification/querying.

Important identity constraint:

```sql
UNIQUE (source_system, order_reference)
```

The same `orderReference` may theoretically exist in both source systems, therefore `order_reference` alone is not treated as globally unique.

Writes use:

```sql
ON CONFLICT (source_system, order_reference) DO NOTHING
```

This makes Kafka replay safe and supports at-least-once processing semantics.

`schema.sql` is executed by Spring Boot at startup using:

```yaml
spring.sql.init.mode: always
```

For production, versioned schema migration tooling such as Flyway or Liquibase would be preferable.

## Batch processing

The normalized consumer uses a Kafka batch listener.

Two separate controls are exposed:

```text
KAFKA_MAX_POLL_RECORDS
DB_BATCH_SIZE
```

Example defaults:

```text
Kafka poll/listener maximum: 500 records
JDBC batch maximum:          100 rows
```

If a Kafka poll returns 420 orders with a DB batch size of 100, persistence can execute approximately:

```text
100 + 100 + 100 + 100 + 20
```

The business transformation still processes each order individually inside the batch. The throughput improvement comes mainly from reducing expensive I/O and framework/database round trips rather than trying to vectorize the Java business logic.

## Failure handling

### Source A / Source B failures

Source-specific listeners use the shared Kafka error handler.

After the configured retry attempts are exhausted, the original record is published to its source-specific DLQ:

```text
orders.source-a.dlq
orders.source-b.dlq
```

### Processing failure inside a normalized Kafka batch

If record `i` fails during deserialization or business processing:

1. records before `i` have already been processed successfully
2. the successful prefix is persisted
3. the listener throws `BatchListenerFailedException` with index `i`
4. Spring Kafka can recover from the exact failed position instead of treating the entire batch as an opaque failure

This keeps the successful prefix aligned with Kafka's ordered offsets.

### Deterministic database row failure

A PostgreSQL data/constraint error may be caused by one bad row inside an otherwise valid JDBC batch.

The sink first tries the batch normally. For deterministic SQL data/integrity failures (`22xxx` / `23xxx` SQL states), it recursively splits the failed range until the first rejected row is isolated.

Example:

```text
[0..99] fails
   |
   +--> [0..49] succeeds
   |
   +--> [50..99] fails
             |
             +--> split again
                     ...
                         |
                         +--> [81] fails
```

The isolated row produces `FailedOrderPersistenceException(index=81)`, which the listener maps to `BatchListenerFailedException(index=81)`.

`FailedOrderPersistenceException` is configured as non-retryable because retrying identical deterministic bad data is not useful. The failed Kafka record is therefore recovered to the normalized DLQ immediately.

If multiple poison records are present in one original Kafka batch, they are discovered in order across subsequent redeliveries. This preserves partition offset ordering and avoids committing past an unresolved failed offset.

### Transient/infrastructure database failure

Examples:

- database unavailable
- connection failure
- transient network issue
- unknown non-data database failure

These failures are not attributed to a specific order. The original database exception is rethrown, causing the uncommitted Kafka batch to be retried.

Previously committed database chunks may be replayed, but the unique constraint plus `ON CONFLICT DO NOTHING` makes replay idempotent.

## Delivery semantics

The design assumes at-least-once processing.

A message may be delivered or processed more than once because of crashes, retries, or offset replay. Correctness is achieved with idempotent persistence rather than by assuming exactly-once execution.

## Scaling

Kafka partitions define the primary horizontal scaling limit.

The raw topics use separate partitions for each source, while the normalized topic has more partitions so common processing can scale independently.

Additional application instances can join the same consumer group and divide partitions between them.

The service is otherwise stateless; durable state lives in Kafka and PostgreSQL.

At higher scale, the main areas to monitor would be:

- Kafka consumer lag
- records processed per second
- batch processing latency
- retry/DLQ rates
- JVM CPU/memory
- PostgreSQL insert latency
- connection-pool saturation
- database lock/contention rates
- WAL/storage throughput

Adding more Kafka consumers is not always useful if PostgreSQL becomes the bottleneck. Database batching, indexes, connection-pool sizing, transaction size, and storage capacity would need to scale together.

## Adding another source

A new source would normally require:

1. a new source DTO
2. a new raw topic/consumer
3. a new `OrderNormalizer<NewSourceDto>` implementation
4. tests for the external schema and normalization

The common `CanonicalOrder`, `OrderProcessor`, country resolver, normalized topic, and persistence layer can remain unchanged if the new source can map into the existing canonical contract.

## Assumptions

- Source events are already published to the defined Kafka raw topics.
- One Kafka record represents one order.
- Source order identifiers are unique only within a source system.
- Country codes are ISO-like two-letter values provided in the configured reference map.
- `orderTimestamp` values are supplied without a timezone offset and are stored as `TIMESTAMP` for the assignment. In production, timezone requirements should be clarified and `Instant`/`TIMESTAMPTZ` may be preferable.
- Zero-priced orders are allowed; negative prices are not.
- Quantity must be greater than zero.
- PostgreSQL is an implementation choice because the assignment does not define target transport.

## Production evolution

If this moved beyond a take-home implementation, the main changes I would consider are:

- use Schema Registry with Avro/Protobuf for Kafka contract versioning
- replace startup `schema.sql` with Flyway/Liquibase migrations
- configure retry policies by exception type, with longer exponential backoff for infrastructure outages
- add structured metrics and alerts for lag, retries, DLQs, DB latency, and batch sizes
- add tracing/correlation IDs across raw and normalized records
- add authentication/encryption for Kafka and PostgreSQL
- define retention and operational workflows for DLQ replay
- make country/reference data dynamically refreshable if ownership changes
- clarify timezone semantics explicitly
- tune partitions, consumer concurrency, poll size, JDBC batch size, and connection pools using measured load
- consider transactional/outbox patterns if a future target requires coordinated writes to multiple systems

## Alternatives considered

### Direct processing without a normalized topic

Simpler for a very small system, but couples each source listener directly to common processing and persistence. The normalized topic was retained as a clean contract and independent scaling/replay boundary.

### One generic source consumer abstraction

Rejected for now. Source A and Source B have genuinely different external contracts, and a generic consumer interface would add abstraction without meaningful shared behavior. Common logic is already centralized after normalization.

### Redis for country lookup

Rejected because three static country mappings do not justify another service. The resolver interface preserves future replaceability.

### JSONB-only persistence

Rejected in favor of explicit relational columns because the target shape is fixed and relational fields are easier to validate and query.

### One database insert per Kafka record

Initially considered for simplicity, then replaced with configurable JDBC batching to better demonstrate scalable I/O handling without introducing a long-lived custom in-memory micro-batch buffer and manual offset coordination.
