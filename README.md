# Node-Assigned Distributed Task System

A secure, distributed task processing system built with **Spring Boot**, **Apache Kafka**, and **MySQL**. Tasks are explicitly assigned to worker nodes and only the assigned node can fetch, execute, and update them.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Server** | Spring Boot 3.5.10, Java 17 |
| **Worker** | Spring Boot 3.5.10, Java 17 |
| **Database** | MySQL 8 |
| **Message Broker** | Apache Kafka |
| **Authentication** | JWT (HS256) via jjwt 0.11.5 |
| **Password Hashing** | BCrypt |
| **Build Tool** | Maven |

---

## Architecture

```
┌─────────────────┐         ┌──────────────┐
│   Admin / API   │◄────►   │  Main Server │
│   Client        │  REST   │  (port 8080) │
└─────────────────┘  +JWT   └──────┬───────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
              ┌──────────┐  ┌──────────┐  ┌──────────┐
              │  MySQL   │  │  Kafka   │  │ Timeout  │
              │ (Storage)│  │ (Notify) │  │  Job     │
              └──────────┘  └────┬─────┘  └──────────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │ Worker 1 │ │ Worker 2 │ │ Worker N │
              │ (Node 1) │ │ (Node 2) │ │ (Node N) │
              └──────────┘ └──────────┘ └──────────┘
```

> See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed diagrams (Mermaid), security flow, task lifecycle state machine, and failover strategy.

---

## Features

- **Secure Node Authentication** — JWT-based auth with BCrypt-hashed secrets
- **Task Ownership Enforcement** — Server verifies requesting node ID matches assigned node ID
- **Status Regression Prevention** — Completed tasks cannot be moved back to earlier states  
- **Idempotent Status Updates** — `oldStatus` parameter prevents double-processing
- **Task Timeout Detection** — Scheduled job marks stale `in_progress` tasks as `timed_out`  
- **Failover & Reassignment** — Admin API to reassign tasks (single or bulk) to different nodes
- **Per-Node Kafka Topics** — Each node gets its own topic (`tasks.node.<id>`), auto-provisioned on startup
- **Role-Based Access** — Separate `admin` and `node` JWT types with different authorities

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8** running on `localhost:3306`
- **Apache Kafka** running on `localhost:9092`

---

## Setup & Run

### 1. Database Setup

```bash
mysql -u root -p < schema.sql
```

Or let Hibernate auto-create tables (default: `spring.jpa.hibernate.ddl-auto=update`).

### 2. Start Kafka

```bash
# Start Kafka Broker
kafka-server-start.bat C:\kafka\config\kraft\server.properties
```

### 3. Start the Server

```bash
cd server
./mvnw spring-boot:run
```

Server starts on **http://localhost:8080**.

### 4. Start Worker Node(s)

```bash
cd worker
./mvnw spring-boot:run
```

Worker starts on a random port. Configure which node it represents in `worker/src/main/resources/application.properties`:

```properties
worker.nodeId=1
worker.nodeSecret=node1Secret
```

To run multiple workers, change the `nodeId` and `nodeSecret` for each instance.

---

## API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/login` | None | Admin/user login → JWT |
| `POST` | `/auth/login/node` | None | Node login → JWT |
| `POST` | `/task/create` | Admin JWT | Create a new task assigned to a node |
| `GET` | `/task/getByTaskId?taskId=` | Node JWT | Get task (only if assigned to requesting node) |
| `GET` | `/task/getAllForNode` | Node JWT | Get all tasks assigned to requesting node |
| `PUT` | `/task/updateStatus?taskId=&oldStatus=&newStatus=` | Node JWT | Update task status (idempotent) |
| `PUT` | `/task/reassignTask?taskId=&nodeId=` | Admin JWT | Reassign a single task to another node |
| `PUT` | `/task/reassignAllForNode?oldNodeId=&newNodeId=` | Admin JWT | Reassign all non-completed tasks |

> See [API_CURL_COLLECTION.md](API_CURL_COLLECTION.md) for ready-to-use cURL commands.

---

## Task Status Flow

```
pending → in_progress → completed  (terminal)
              │
              └──────→ failed
              └──────→ timed_out

timed_out / failed → pending  (via admin reassign)
completed → BLOCKED  (no further transitions allowed)
```

---

## Project Structure

```
distributed-task-system/
├── server/                           # Main Server (Spring Boot)
│   └── src/main/java/task/server/
│       ├── ServerApplication.java    # Entry point (@EnableScheduling)
│       ├── config/
│       │   └── KafkaProducerConfig   # Kafka producer (idempotent, acks=all)
│       ├── controller/
│       │   ├── AuthController        # /auth/login, /auth/login/node
│       │   └── TaskController        # /task/** CRUD + reassign
│       ├── dto/
│       │   ├── CreateTaskDto
│       │   ├── GetTaskDto
│       │   ├── NodeLoginRequestDto
│       │   ├── NodeLoginResponseDto
│       │   ├── UserLoginRequestDto
│       │   └── UserLoginResponseDto
│       ├── entity/
│       │   ├── Node                  # Worker node identity
│       │   ├── Task                  # Task with UUID, status, timestamps
│       │   ├── TaskStatus            # Enum: pending|in_progress|completed|failed|timed_out
│       │   └── User                  # Admin user
│       ├── exception/
│       │   └── GlobalExceptionHandler
│       ├── job/
│       │   └── TaskTimeoutJob        # Cron: marks stale tasks as timed_out
│       ├── producer/
│       │   └── KafkaPublisher        # Publishes to tasks.node.<id>
│       ├── repository/
│       │   ├── NodeRepository
│       │   ├── TaskRepository
│       │   └── UserRepository
│       ├── security/
│       │   ├── JwtAuthFilter         # Validates JWT on every request
│       │   ├── JwtService            # Mint + parse JWT tokens
│       │   ├── NodeAuthentication    # Custom auth object with ROLE_NODE
│       │   └── SecurityConfig        # Spring Security config
│       ├── service/
│       │   ├── AuthService           # Login logic + BCrypt validation
│       │   └── TaskService           # Business logic + ownership checks
│       └── utility/
│           └── NodeTopicProvisioner  # Auto-creates Kafka topics on startup
│
├── worker/                           # Worker Node (Spring Boot)
│   └── src/main/java/task/worker/
│       ├── WorkerApplication.java    # Entry point (@EnableKafka)
│       ├── config/
│       │   ├── KafkaConsumerConfig   # Per-node consumer group + topic
│       │   └── RestTemplateConfig    # REST client bean
│       ├── consumer/
│       │   └── TaskConsumer          # Kafka listener: fetch → execute → update
│       ├── dto/
│       │   └── GetTaskDto
│       ├── executor/
│       │   └── TaskExecutor          # Executes by type (EMAIL, PDF, etc.)
│       └── utility/
│           ├── ServerApiClient       # REST calls to server with JWT retry
│           └── TokenManager          # JWT lifecycle: login, cache, refresh
│
├── ARCHITECTURE.md                   # Detailed architecture documentation
├── API_CURL_COLLECTION.md            # cURL commands for all endpoints
├── schema.sql                        # MySQL DDL + seed data
└── README.md                         # This file
```

---

## Configuration

### Server (`server/src/main/resources/application.properties`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/node_tasks` | MySQL connection |
| `spring.datasource.username` | `root` | DB username |
| `spring.datasource.password` | `test` | DB password |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker |
| `app.jwt.secret` | `CHANGE_ME_...` | JWT signing key (64+ chars) |
| `app.jwt.ttlSeconds` | `3600` | JWT token lifetime (1 hour) |
| `app.task.timeoutSeconds` | `900` | In-progress timeout (15 min) |

### Worker (`worker/src/main/resources/application.properties`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `0` | Random port (worker doesn't serve HTTP) |
| `worker.serverBaseUrl` | `http://localhost:8080` | Server URL |
| `worker.nodeId` | `1` | Node identity |
| `worker.nodeSecret` | `node1Secret` | Node auth secret |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker |

---

## Deliverables

| # | Deliverable | File |
|---|---|---|
| 1 | Architecture Overview | [ARCHITECTURE.md](ARCHITECTURE.md) |
| 2 | API Definitions (cURL) | [API_CURL_COLLECTION.md](API_CURL_COLLECTION.md) |
| 3 | Data Schema | [schema.sql](schema.sql) |
| 4 | Implementation | `server/` and `worker/` directories |
| 5 | README | This file |
