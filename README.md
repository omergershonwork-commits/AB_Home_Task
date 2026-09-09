# AB Home Task - Multi-Source Order Processing

Java/Spring Boot service that receives two order schemas from Kafka, normalizes them, applies shared business logic, and stores the final result in PostgreSQL.

## Stack
Java 21, Spring Boot 3.5.6, Spring Kafka, PostgreSQL 16, Maven, Docker Compose.

## Flow
```text
Source A -> orders.source-a.raw -> normalize --\
                                           -> orders.normalized -> process -> PostgreSQL
Source B -> orders.source-b.raw -> normalize --/
```

## Run
```powershell
mvn test
docker compose up -d
mvn spring-boot:run
```

Health check:
```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

## Test Source A
```powershell
docker compose exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9092 --topic orders.source-a.raw
```
Paste:
```json
{"orderId":"ORD-A-1","customerId":"CUST-501","customerName":"John Smith","country":"US","orderDate":"2026-09-01T10:30:00","productCode":"P100","quantity":2,"unitPrice":125.50}
```

## Test Source B
```powershell
docker compose exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9092 --topic orders.source-b.raw
```
Paste:
```json
{"order_number":"ORD-B-1","customer":{"id":"CUST-842","first_name":"Jane","last_name":"Miller","country_code":"DE"},"created_at":"2026-09-01T11:15:00","item":{"sku":"P200","units":3,"price":80.00}}
```

## View Output
```powershell
docker compose exec postgres psql -U order_app -d order_processing -c "SELECT source_system, order_reference, customer_full_name, country, currency, total_order_value FROM processed_orders ORDER BY id DESC;"
```

Expected examples:
```text
SOURCE_A | ORD-A-1 | John Smith  | United States | USD | 251.00
SOURCE_B | ORD-B-1 | Jane Miller | Germany       | EUR | 240.00
```

## Failure Handling
Invalid source records go to the source DLQ. Deterministic database row failures are isolated and sent to `orders.normalized.dlq`. Transient database failures are retried.

Database writes are idempotent using `UNIQUE (source_system, order_reference)` and `ON CONFLICT DO NOTHING`.

## Batch Configuration
Defaults: Kafka poll max `500`, JDBC batch size `100`.

Override with:
```powershell
$env:KAFKA_MAX_POLL_RECORDS="200"
$env:DB_BATCH_SIZE="50"
```

See `DESIGN.md` for design decisions and `AI_USAGE.md` for AI usage.
