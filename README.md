# AB Home Task - Multi-Source Order Processing

A focused Java/Spring Boot service that consumes two different order schemas from Kafka, normalizes them into one canonical model, enriches and transforms the orders, then persists the final result to PostgreSQL.

## Stack

- Java 21
- Spring Boot 3.5.6
- Spring Kafka
- PostgreSQL 16
- Docker Compose
- Maven

Kafka is the service ingestion boundary. The mechanism that publishes Source A and Source B events into Kafka is outside this service's scope.

## Data flow

```text
Source A -> orders.source-a.raw -> SourceAOrderNormalizer --\
                                                         -> orders.normalized -> OrderProcessor -> PostgreSQL
Source B -> orders.source-b.raw -> SourceBOrderNormalizer --/
```

Failures are routed to source-specific DLQs:

- `orders.source-a.dlq`
- `orders.source-b.dlq`
- `orders.normalized.dlq`

The normalized consumer receives Kafka poll results as batches. PostgreSQL writes are also batched, with separate configurable Kafka and JDBC batch sizes.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop / Docker Compose

## Build and test

```powershell
mvn test
```

## Run locally

Start Kafka and PostgreSQL:

```powershell
docker compose up -d
```

Check the containers:

```powershell
docker compose ps -a
```

`kafka` and `postgres` should be healthy. `kafka-init` is a one-shot container and should finish with `Exited (0)` after creating the topics.

Verify the Kafka topics:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

Expected application topics:

```text
orders.normalized
orders.normalized.dlq
orders.source-a.dlq
orders.source-a.raw
orders.source-b.dlq
orders.source-b.raw
```

Run the Spring Boot application from the repository root:

```powershell
mvn spring-boot:run
```

Verify application health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

The response should report `UP`.

## Verify the PostgreSQL schema

The application executes `src/main/resources/schema.sql` on startup.

```powershell
docker compose exec postgres psql `
  -U order_app `
  -d order_processing `
  -c "\d processed_orders"
```

The table contains the transformed order fields plus internal `source_system` metadata. `(source_system, order_reference)` is unique so replayed Kafka records are idempotent.

## Demo Source A

Start a producer:

```powershell
docker compose exec -i kafka /opt/kafka/bin/kafka-console-producer.sh `
  --bootstrap-server kafka:9092 `
  --topic orders.source-a.raw
```

Paste one line:

```json
{"orderId":"ORD-DEMO-A-1","customerId":"CUST-501","customerName":"John Smith","country":"US","orderDate":"2026-09-01T10:30:00","productCode":"P100","quantity":2,"unitPrice":125.50}
```

Query the result:

```powershell
docker compose exec postgres psql `
  -U order_app `
  -d order_processing `
  -c "SELECT source_system, order_reference, customer_full_name, country, currency, quantity, unit_price, total_order_value FROM processed_orders WHERE order_reference='ORD-DEMO-A-1';"
```

Expected business values:

```text
SOURCE_A | ORD-DEMO-A-1 | John Smith | United States | USD | 2 | 125.50 | 251.00
```

## Demo Source B

Start a producer:

```powershell
docker compose exec -i kafka /opt/kafka/bin/kafka-console-producer.sh `
  --bootstrap-server kafka:9092 `
  --topic orders.source-b.raw
```

Paste one line:

```json
{"order_number":"ORD-DEMO-B-1","customer":{"id":"CUST-842","first_name":"Jane","last_name":"Miller","country_code":"DE"},"created_at":"2026-09-01T11:15:00","item":{"sku":"P200","units":3,"price":80.00}}
```

Query the result:

```powershell
docker compose exec postgres psql `
  -U order_app `
  -d order_processing `
  -c "SELECT source_system, order_reference, customer_full_name, country, currency, total_order_value FROM processed_orders WHERE order_reference='ORD-DEMO-B-1';"
```

Expected business values:

```text
SOURCE_B | ORD-DEMO-B-1 | Jane Miller | Germany | EUR | 240.00
```

## Verify idempotency

Publish the same `ORD-DEMO-A-1` message again, then run:

```powershell
docker compose exec postgres psql `
  -U order_app `
  -d order_processing `
  -c "SELECT COUNT(*) FROM processed_orders WHERE source_system='SOURCE_A' AND order_reference='ORD-DEMO-A-1';"
```

Expected count: `1`.

## View normalized output

To inspect normalized messages directly:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:9092 `
  --topic orders.normalized `
  --property print.key=true `
  --property key.separator=" -> "
```

The Kafka key is `sourceSystem:orderReference`, for example:

```text
SOURCE_A:ORD-DEMO-A-1
```

## Verify DLQ behavior

Watch the normalized DLQ:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:9092 `
  --topic orders.normalized.dlq `
  --from-beginning
```

A deterministic PostgreSQL row/data failure is isolated to the exact bad order and sent directly to the normalized DLQ. Transient infrastructure failures, such as PostgreSQL being unavailable, remain retryable batch failures.

Source-specific deserialization/normalization failures are routed to the corresponding source DLQ after the configured retry policy.

## Batch configuration

Defaults:

```yaml
spring.kafka.consumer.max-poll-records: 500
app.persistence.batch-size: 100
```

Override them for a local run:

```powershell
$env:KAFKA_MAX_POLL_RECORDS="200"
$env:DB_BATCH_SIZE="50"
mvn spring-boot:run
```

`KAFKA_MAX_POLL_RECORDS` is the maximum number of Kafka records returned by one poll/listener invocation. `DB_BATCH_SIZE` is the maximum number of rows in one JDBC batch.

Clear the overrides afterward:

```powershell
Remove-Item Env:KAFKA_MAX_POLL_RECORDS
Remove-Item Env:DB_BATCH_SIZE
```

## Documentation

- `DESIGN.md` - architecture, decisions, assumptions, failure handling, and production evolution
- `AI_USAGE.md` - meaningful AI usage during implementation and which recommendations were accepted, modified, or rejected

## Stop local infrastructure

```powershell
docker compose down
```

To also delete the PostgreSQL volume:

```powershell
docker compose down -v
```
