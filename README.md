# AB Home Task - Multi-Source Order Processing

Stage 1 contains the application and infrastructure foundation only. Business processing will be added in the next stages.

## Stack

- Java 21
- Spring Boot 3.5.6
- Spring Kafka
- PostgreSQL 16
- Docker Compose

Kafka is the ingestion boundary. No source REST API is implemented.

## Kafka topics

- `orders.source-a.raw` - 3 partitions
- `orders.source-b.raw` - 3 partitions
- `orders.normalized` - 6 partitions
- `orders.source-a.dlq` - 3 partitions
- `orders.source-b.dlq` - 3 partitions
- `orders.normalized.dlq` - 6 partitions

The normalized topic is a separate scaling boundary so normalization and common order processing can scale independently.

DLQ topics are provisioned now as infrastructure. The consumer error-handling logic that routes failed records to them will be added together with the consumers in the next stages.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop / Docker Compose

## Run Stage 1

Start Kafka and PostgreSQL:

```bash
docker compose up -d
```

Check the containers:

```bash
docker compose ps
```

Verify the Kafka topics:

```bash
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

Verify PostgreSQL:

```bash
docker compose exec postgres psql -U order_app -d order_processing -c "SELECT 1;"
```

Run the Spring Boot application from the repository root:

```bash
mvn spring-boot:run
```

Verify application health:

```bash
curl http://localhost:8080/actuator/health
```

On PowerShell you can also use:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

The health response should report `UP`, including PostgreSQL and Kafka connectivity.

Stop the infrastructure:

```bash
docker compose down
```

To also remove the local PostgreSQL data volume:

```bash
docker compose down -v
```
