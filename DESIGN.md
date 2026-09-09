# Design

## Architecture
```text
Source A -> raw topic -> SourceA normalizer --\
                                            -> normalized topic -> OrderProcessor -> PostgreSQL
Source B -> raw topic -> SourceB normalizer --/
```

Kafka is the service input boundary. How source systems publish into Kafka is outside this service.

## Main Decisions
- Each source has its own DTO and `OrderNormalizer<T>` because the external schemas are different.
- Both sources become one `CanonicalOrder`, so shared business logic is implemented once.
- `OrderProcessor` validates, resolves country/currency, and calculates `quantity * unitPrice`.
- `BigDecimal` is used for money.
- Country data is loaded from `country-reference.yml`; Redis or another service would be unnecessary for three static values.
- PostgreSQL is used as the target because the assignment defines the output shape but not the target transport.
- `TargetOrderSink` keeps persistence replaceable.
- Kafka and JDBC processing are batched separately using configurable limits.

## Reliability
The design assumes at-least-once delivery.

`UNIQUE (source_system, order_reference)` plus `ON CONFLICT DO NOTHING` makes database writes idempotent.

Source parsing/normalization failures use retry and then their source DLQ.

For the normalized batch:
- a processing failure reports the exact Kafka batch index with `BatchListenerFailedException`;
- a deterministic PostgreSQL data/constraint failure is isolated to the exact row and sent directly to the normalized DLQ;
- a transient database failure is not assigned to one order and the uncommitted batch is retried.

## Scaling
Kafka partitions allow multiple consumers to process in parallel. The service itself is stateless; durable state is in Kafka and PostgreSQL.

The expected 50,000 orders/day does not require Kafka for throughput alone. Kafka was chosen mainly for buffering, replay, failure isolation, and independent scaling.

## Assumptions
- One Kafka record represents one order.
- Source order IDs are unique only inside their source system.
- PostgreSQL is an implementation choice, not a requirement stated by the assignment.
- Country mappings are the three values supplied in the task.
- Quantity must be positive and price cannot be negative.

## Alternatives
Direct processing without `orders.normalized` would be simpler but would couple source handling to shared processing.

A generic source consumer, Redis country lookup, JSONB-only storage, and a custom timed micro-batch buffer were rejected because they add complexity without enough value for this task.

## Production
For production I would consider Schema Registry, Flyway/Liquibase, stronger observability, security, longer backoff for infrastructure failures, DLQ replay tooling, timezone clarification, and tuning partitions/batch sizes from measured load.
