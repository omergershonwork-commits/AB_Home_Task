# AI Usage

AI was used as a design and implementation assistant during the assignment. Recommendations were reviewed rather than applied blindly. The examples below show where AI materially influenced the solution and whether the recommendation was accepted, modified, or rejected.

## 1. Separate source normalization from common business processing

### AI recommendation

Introduce a source-specific normalization layer:

```java
public interface OrderNormalizer<T> {
    CanonicalOrder normalize(T sourceOrder);
}
```

with one implementation for Source A and one for Source B, then process a shared `CanonicalOrder` downstream.

### Decision

**Accepted.**

### Why

The two source contracts have different field names and nesting, but the downstream business rules are shared. Keeping source mapping separate prevents source-specific JSON details from leaking into the common processor.

It also makes adding another source straightforward: add a DTO, consumer, and normalizer without changing the shared processing logic.

## 2. Use Kafka raw topics and a normalized topic as separate boundaries

### AI recommendation

Use:

```text
orders.source-a.raw
orders.source-b.raw
orders.normalized
```

with source-specific consumers publishing the common canonical representation to the normalized topic.

### Decision

**Accepted with scope control.**

### Why

Kafka gives buffering, replay, failure isolation, and independent scaling between normalization and common processing.

However, the recommendation was kept inside one Spring Boot deployment for the take-home. Splitting every logical stage into a separate microservice was rejected because the expected volume does not justify the additional deployment and operational complexity.

## 3. Country enrichment abstraction

### AI recommendation

Hide country lookup behind:

```java
public interface CountryResolver {
    CountryInfo resolve(String countryCode);
}
```

and consider a cache/database-backed implementation for production.

### Decision

**Interface accepted; Redis/database lookup rejected for the current assignment.**

### Why

The abstraction is useful because reference-data ownership may change later.

The actual assignment contains only three static mappings (`US`, `GB`, `DE`), so introducing Redis or another external datastore would be unnecessary complexity. The current implementation loads the mappings from `country-reference.yml`.

## 4. PostgreSQL as the target sink

### AI recommendation

Because the assignment defines the target data shape but not the target transport, use PostgreSQL as a concrete durable sink and put it behind a persistence interface.

### Decision

**Accepted.**

### Why

PostgreSQL makes the transformed output easy to verify and demonstrates durable processing without coupling the business layer directly to JDBC.

`TargetOrderSink` leaves room to replace PostgreSQL with another adapter later.

A JSONB-only storage design was considered but rejected because the output shape is fixed and explicit relational columns are easier to query and validate.

## 5. Idempotency for Kafka replay

### AI recommendation

Assume at-least-once delivery rather than exactly-once execution and make the database write idempotent with:

```sql
UNIQUE (source_system, order_reference)
```

and:

```sql
ON CONFLICT (source_system, order_reference) DO NOTHING
```

### Decision

**Accepted.**

### Why

Kafka retries or consumer restarts can cause replay. The same order reference could also theoretically exist in both source systems, so `(sourceSystem, orderReference)` is a safer identity than `orderReference` alone.

The assignment target JSON does not contain `sourceSystem`, therefore that value remains internal persistence metadata rather than being added to `ProcessedOrder`.

## 6. Persistence batching

### Initial AI recommendation

Keep persistence simple with one database save per processed Kafka record because 50,000 orders/day is modest average throughput.

### Decision

**Modified after review.**

### Why

Although the stated volume is not large, individual database writes are not a good scalable I/O pattern. The design was changed to:

```text
Kafka batch listener
    -> process records
    -> JDBC batch persistence
```

with separate configurable limits:

```text
KAFKA_MAX_POLL_RECORDS
DB_BATCH_SIZE
```

This preserves a simple bounded lifecycle while reducing database round trips.

A custom long-lived micro-batching buffer (`X records or Y milliseconds`) was discussed but rejected because correct implementation would require manual offset/acknowledgement coordination, rebalance handling, shutdown flushing, and additional concurrency logic.

## 7. Batch failure handling with `BatchListenerFailedException`

### AI recommendation

When one record inside a Kafka listener batch fails during parsing or business processing, throw `BatchListenerFailedException` with the exact failed index instead of treating the complete batch as an opaque failure.

### Decision

**Accepted.**

### Why

This allows the successful contiguous prefix to be persisted before the failed record is reported, while keeping Kafka offset handling aligned with the actual point of failure.

It avoids sending an entire otherwise-valid Kafka batch to a DLQ because of one poison record.

## 8. Database-side poison-record isolation

### AI recommendation

A JDBC batch can fail because one row violates a deterministic PostgreSQL data/constraint rule. First attempt the batch normally, then isolate a deterministic failure by recursively splitting the failed range until the exact row is found.

### Decision

**Accepted and refined.**

### Why

Retrying or DLQing a whole batch because one row is invalid would discard or repeatedly reprocess valid work.

The implementation only isolates failures that appear deterministic (`22xxx` data errors and `23xxx` integrity-constraint errors). Infrastructure failures such as connection loss are not attributed to one record; they remain retryable batch failures.

Once a deterministic bad row is isolated, `FailedOrderPersistenceException` carries its batch index back to the Kafka listener, which converts it into `BatchListenerFailedException`.

## 9. Retry policy for deterministic failures

### Initial AI behavior

Use the same fixed Kafka retry policy for all failures before sending the record to DLQ.

### Decision

**Modified.**

### Why

Retrying identical deterministic bad data is wasteful. After a PostgreSQL row has already been isolated as a deterministic failure, the same unchanged record is not expected to succeed on retry.

`FailedOrderPersistenceException` is therefore configured as non-retryable and is recovered directly to the normalized DLQ.

Transient/infrastructure database failures still use the normal retry policy.

## 10. Production recommendations

AI also suggested several production-oriented improvements that were **documented but intentionally not implemented** because they are outside the focused take-home scope:

- Schema Registry with Avro/Protobuf
- Flyway/Liquibase migrations
- dynamic reference-data service/cache
- Kubernetes and separate microservice deployments
- distributed tracing
- advanced metrics/alerting
- long-lived custom persistence buffers
- additional datastores such as Redis, MongoDB, or ClickHouse

These are valid considerations for a larger system, but implementing them here would reduce clarity and exceed the assignment's goal of a focused solution.

## Summary

AI was most useful for:

- comparing architecture alternatives
- identifying failure/retry edge cases
- reviewing Kafka offset and batch semantics
- suggesting test scenarios
- drafting documentation structure

The final implementation deliberately keeps several AI suggestions out of scope where they added more complexity than value.
