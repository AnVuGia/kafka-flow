# Kafka Sample Repository

A ready-to-use Kafka setup with Docker Compose running in **KRaft mode** (Kafka Raft), eliminating the need for Zookeeper. Includes Kafka UI for easy cluster management.

## Features

- **Apache Kafka** running in a container with **KRaft mode** (no Zookeeper required!)
- **KRaft (Kafka Raft)**: Uses Kafka's built-in metadata management - simpler and more efficient
- **Kafka UI**: Web-based interface for managing topics, messages, and cluster operations
- **Configurable cluster ID**: Set your own cluster identifier via environment variables
- **Easy to use**: Just run `docker compose up` and everything is configured

## Prerequisites

- Docker
- Docker Compose (or `docker compose` command)

## Project Structure

```
Kafka_sample/
├── docker-compose.yml         # Docker Compose configuration
├── server-kraft.properties    # Kafka KRaft mode configuration
├── README.md                  # This file
└── data/kafka/                # Kafka data directory (created on first run)
```

## Quick Start

1. **Start the Kafka cluster:**
   ```bash
   docker compose up -d
   ```
   
   **Or customize the cluster ID:**
   ```bash
   KAFKA_CLUSTER_ID=MyCustomClusterID docker compose up -d
   ```

2. **Access Kafka UI:**
   - Open your browser and navigate to `http://localhost:8080`
   - You should see the "kraft-cluster" with your Kafka broker

3. **Verify the setup:**
   ```bash
   docker exec kafka-kraft-1 /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
   ```

4. **Stop the cluster:**
   ```bash
   docker compose down
   ```

## Cluster Configuration

### Cluster ID

The cluster ID identifies your Kafka cluster. All brokers with the same cluster ID belong to the same cluster.

**Set a custom cluster ID:**
```bash
KAFKA_CLUSTER_ID=MyCustomClusterID docker compose up -d
```

**Or create a `.env` file:**
```bash
KAFKA_CLUSTER_ID=MyCustomClusterID
```

Default cluster ID: `MkU3OEVBNTcwNTJENDM2Qk`

### Configuration Files

- **`server-kraft.properties`**: Main Kafka configuration file for KRaft mode
  - Defines listeners, ports, and cluster settings
  - Mounted into the container at `/opt/kafka/config/kraft/server.properties`

## Useful Commands

### List all topics
```bash
docker exec kafka-kraft-1 /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Create a topic
```bash
docker exec kafka-kraft-1 /opt/kafka/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic your-topic-name
```

### Describe a topic
```bash
docker exec kafka-kraft-1 /opt/kafka/bin/kafka-topics.sh --describe --topic your-topic-name --bootstrap-server localhost:9092
```

### Produce messages
```bash
docker exec -it kafka-kraft-1 /opt/kafka/bin/kafka-console-producer.sh --topic your-topic-name --bootstrap-server localhost:9092
```

### Consume messages
```bash
docker exec -it kafka-kraft-1 /opt/kafka/bin/kafka-console-consumer.sh --topic your-topic-name --from-beginning --bootstrap-server localhost:9092
```

### Using Kafka UI

Access the web interface at `http://localhost:8080` to:
- View and manage topics
- Browse and produce messages
- Monitor cluster health
- View consumer groups

## Connection Details

- **Kafka Broker (External)**: `localhost:19092`
- **Kafka Broker (Internal)**: `kafka-1:9094` (for container-to-container communication)
- **Controller Port**: `9093` (internal only, for KRaft controller communication)
- **Kafka UI**: `http://localhost:8080`

### For Applications

- **Local applications**: Use `localhost:19092`
- **Docker containers in same network**: Use `kafka-1:9094`

## Architecture

The setup uses **KRaft mode** (Kafka Raft), which means:
- **No Zookeeper required**: Kafka manages its own metadata using the Raft consensus algorithm
- **Kafka Broker/Controller**: Single node (`kafka-1`) running as both broker and controller (node.id=1)
- **Kafka UI**: Web interface for cluster management and monitoring
- **Simpler setup**: Fewer moving parts, faster startup, better performance
- **Persistent storage**: Data is stored in a Docker volume (`kafka-data-1`)

### Services

1. **kafka-1** (Container: `kafka-kraft-1`): Main Kafka broker running in KRaft mode
   - **External Port**: `19092` (for local applications)
   - **Internal Port**: `9094` (for container-to-container communication)
   - **Controller Port**: `9093` (internal only, for KRaft controller communication)
   - Uses `server-kraft.properties` configuration file
   - Automatically formats storage with cluster ID on first run
   - Health check ensures Kafka is ready before dependent services start

2. **kafka-ui**: Web-based Kafka management interface
   - **Port**: `8080`
   - Connects to `kraft-cluster` at `kafka-1:9094`
   - Provides topic management, message browsing, and cluster monitoring

### Configuration

- **Configuration File**: `server-kraft.properties`
  - Defines all Kafka settings (listeners, ports, replication, etc.)
  - Mounted into the container at `/opt/kafka/config/kraft/server.properties`
  
- **Cluster ID**: Set via `KAFKA_CLUSTER_ID` environment variable
  - Default: `MkU3OEVBNTcwNTJENDM2Qk`
  - Override with environment variable or `.env` file
  - Used when formatting storage on first startup

- **Network**: `kafka-net` (bridge network for inter-container communication)

- **Volumes**: `kafka-data-1` (persists Kafka data across container restarts)

## Troubleshooting

### Check Kafka logs
```bash
docker compose logs kafka-1
```

### Check Kafka UI logs
```bash
docker compose logs kafka-ui
```

### View all logs
```bash
docker compose logs
```

### Follow logs in real-time
```bash
docker compose logs -f
```

### Restart everything (clean slate)
```bash
docker compose down -v
docker compose up -d
```
**Note**: The `-v` flag removes volumes, so all data will be lost.

### Check if services are running
```bash
docker compose ps
```

### Check Kafka health
```bash
docker exec kafka-kraft-1 /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### Access Kafka container shell
```bash
docker exec -it kafka-kraft-1 /bin/bash
```

### Common Issues

1. **Container fails to start**: 
   - Check if ports 19092 and 8080 are already in use
   - Check logs: `docker compose logs kafka-1`

2. **Storage format errors**: 
   - Remove the volume and restart: `docker compose down -v && docker compose up -d`
   - Check that the cluster ID is set correctly

3. **Kafka UI can't connect**:
   - Verify Kafka is healthy: `docker compose ps`
   - Check that Kafka UI is using the correct bootstrap server: `kafka-1:9094`
   - Check network connectivity: `docker network inspect kafka_sample_kafka-net`

4. **Permission errors**:
   - Ensure the `data/kafka` directory has proper permissions
   - On Windows/WSL, check volume mount paths are correct
