# Kafka Producer Sample Project

A sample Spring Boot application demonstrating how to produce and consume messages with Apache Kafka using `KafkaTemplate`.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose

## Quick Start

### 1. Start All Services with Docker Compose

```bash
docker compose up -d --build
```

This starts:
| Service | Port | Description |
|---------|------|-------------|
| `kafka-1` | 19092 | Apache Kafka broker (KRaft mode) |
| `kafka-producer` | 8080 | Spring Boot producer application |
| `kafka-ui` | 8090 | Web UI for monitoring Kafka |

### 2. Verify Services are Running

```bash
docker compose ps
```

### 3. Access the Services

- **Producer API**: http://localhost:8080
- **Kafka UI**: http://localhost:8090

## API Endpoints

### Producer Endpoints

#### 1. Send Simple String Message

```bash
curl -X POST "http://localhost:8080/api/kafka/send?message=Hello%20World"
```

#### 2. Send Message with Key

```bash
curl -X POST "http://localhost:8080/api/kafka/send-with-key?key=user123&message=Hello%20User"
```

#### 3. Send JSON Message

```bash
curl -X POST "http://localhost:8080/api/kafka/send-json" \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello from JSON", "sender": "John Doe"}'
```

#### 4. Send to Custom Topic

```bash
curl -X POST "http://localhost:8080/api/kafka/send-to-topic?topic=my-custom-topic&key=key1&message=Custom%20message"
```

### Consumer Endpoints

#### 5. List All Topics

```bash
curl http://localhost:8080/api/kafka/topics
```

#### 6. Get Topic Info (partitions, offsets, message count)

```bash
curl http://localhost:8080/api/kafka/topics/string-messages/info
```

#### 7. Read Messages from a Topic

```bash
# Read up to 100 messages from beginning
curl "http://localhost:8080/api/kafka/messages/string-messages"

# Read last 10 messages
curl "http://localhost:8080/api/kafka/messages/string-messages?max=10&fromBeginning=false"

# Read JSON messages
curl "http://localhost:8080/api/kafka/messages/json-messages"
```

**Response Example:**
```json
[
  {
    "topic": "string-messages",
    "partition": 0,
    "offset": 0,
    "key": null,
    "value": "Hello World",
    "timestamp": "2025-12-04T21:30:15"
  }
]
```

### Health Check

```bash
curl http://localhost:8080/api/kafka/health
```

## Project Structure

```
kafka-producer/
├── docker-compose.yml                    # Docker Compose for all services
├── Dockerfile                            # Multi-stage build for Spring Boot app
├── server-kraft.properties               # Kafka KRaft configuration
├── pom.xml                               # Maven dependencies
└── src/main/java/com/example/
    ├── App.java                          # Main application entry point
    ├── config/
    │   ├── KafkaProducerConfig.java      # Kafka producer configuration
    │   └── KafkaConsumerConfig.java      # Kafka consumer configuration
    ├── controller/
    │   └── KafkaProducerController.java  # REST API endpoints
    ├── dto/
    │   ├── MessageRequest.java           # Request DTO for JSON messages
    │   └── KafkaMessageDto.java          # Response DTO for consumed messages
    ├── model/
    │   └── Message.java                  # Message model with metadata
    └── service/
        ├── KafkaProducerService.java     # Producer service using KafkaTemplate
        └── KafkaConsumerService.java     # Consumer service for reading messages
```

## Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `spring.kafka.bootstrap-servers` | Kafka broker address | `localhost:9092` |
| `spring.kafka.consumer.group-id` | Consumer group ID | `kafka-producer-group` |
| `app.kafka.topic.string` | Topic for string messages | `string-messages` |
| `app.kafka.topic.json` | Topic for JSON messages | `json-messages` |

### Docker Compose Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_CLUSTER_ID` | KRaft cluster ID | `MkU3OEVBNTcwNTJENDM2Qk` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers for producer | `kafka-1:9094` |

## Features Demonstrated

### KafkaTemplate (Producer)
1. **Simple message sending** - Fire and forget with async callbacks
2. **Key-based partitioning** - Send messages with keys for ordered processing
3. **JSON serialization** - Send complex objects as JSON
4. **Custom topic routing** - Send to any topic dynamically
5. **Synchronous sending** - Block until acknowledgment received

### KafkaConsumer (Consumer)
1. **List topics** - Get all available topics in the cluster
2. **Topic info** - Get partition count, offsets, and message count
3. **Read messages** - Consume messages from any topic via REST API

## Development

### Run Locally (without Docker)

1. Start Kafka separately on `localhost:9092`

2. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
```

Tests use `@EmbeddedKafka` for in-memory Kafka testing.

## Docker Commands

```bash
# Start all services
docker compose up -d --build

# View logs
docker compose logs -f

# View specific service logs
docker compose logs -f kafka-producer

# Stop all services
docker compose down

# Stop and remove volumes
docker compose down -v

# Rebuild only the producer
docker compose up -d --build kafka-producer
```

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  REST Client    │────▶│  Kafka Producer │◀───▶│  Apache Kafka   │
│  (curl/browser) │     │  (Spring Boot)  │     │  (KRaft mode)   │
│                 │     │  :8080          │     │  :9094          │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │                        │
                               │ produce/consume        │
                               ▼                        ▼
                        ┌─────────────────┐     ┌─────────────────┐
                        │  KafkaTemplate  │     │    Kafka UI     │
                        │  KafkaConsumer  │     │    :8090        │
                        └─────────────────┘     └─────────────────┘
```

## License

MIT
