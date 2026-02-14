# Architecture Overview — Node-Assigned Distributed Task System

## High-Level System Diagram

```mermaid
graph TB
    subgraph Clients
        ADMIN["Admin / API Client"]
    end

    subgraph Main Server ["Main Server (Spring Boot :8080)"]
        AUTH["AuthController<br/>/auth/**"]
        TASK["TaskController<br/>/task/**"]
        JWT["JwtService"]
        SEC["SecurityConfig + JwtAuthFilter"]
        SVC["TaskService"]
        TIMEOUT["TaskTimeoutJob<br/>(Scheduled Cron)"]
        PUB["KafkaPublisher"]
        PROV["NodeTopicProvisioner"]
    end

    subgraph Database ["MySQL (node_tasks)"]
        T_NODE["node"]
        T_TASK["task"]
        T_USER["user"]
    end

    subgraph MessageBroker ["Apache Kafka"]
        TOPIC1["tasks.node.1"]
        TOPIC2["tasks.node.2"]
        TOPIC3["tasks.node.3"]
        TOPIC4["tasks.node.4"]
    end

    subgraph Workers ["Worker Nodes (Spring Boot)"]
        W1["Worker Node 1"]
        W2["Worker Node 2"]
        W3["Worker Node 3"]
        W4["Worker Node 4"]
    end

    ADMIN -->|"REST + JWT"| AUTH
    ADMIN -->|"REST + JWT"| TASK
    TASK --> SEC --> JWT
    TASK --> SVC
    SVC --> T_TASK
    SVC --> T_NODE
    AUTH --> T_NODE
    AUTH --> T_USER
    AUTH --> JWT
    TIMEOUT -->|"Mark timed_out"| T_TASK
    SVC --> PUB
    PROV -->|"Create topics on startup"| MessageBroker
    PUB --> TOPIC1
    PUB --> TOPIC2
    PUB --> TOPIC3
    PUB --> TOPIC4
    TOPIC1 --> W1
    TOPIC2 --> W2
    TOPIC3 --> W3
    TOPIC4 --> W4
    W1 -->|"REST + JWT"| TASK
    W2 -->|"REST + JWT"| TASK
    W3 -->|"REST + JWT"| TASK
    W4 -->|"REST + JWT"| TASK
```

---

## Components

### 1. Main Server (`server/`)

| Component | Class | Responsibility |
|---|---|---|
| **Auth Controller** | `AuthController` | User login (`/auth/login`) and node login (`/auth/login/node`) |
| **Task Controller** | `TaskController` | CRUD + status update + reassignment APIs under `/task/**` |
| **Task Service** | `TaskService` | Business logic: create tasks, ownership-scoped fetch, idempotent status updates, reassignment |
| **Auth Service** | `AuthService` | Validates credentials (BCrypt), mints JWT tokens via `JwtService` |
| **JWT Service** | `JwtService` | Mints & parses JWT tokens (HS256). Separate claims for `node` vs `user` types |
| **JWT Auth Filter** | `JwtAuthFilter` | Intercepts all non-auth requests, validates JWT, sets `SecurityContext` |
| **Node Authentication** | `NodeAuthentication` | Custom `Authentication` object carrying `nodeId` with `ROLE_NODE` authority |
| **Security Config** | `SecurityConfig` | Configures Spring Security: `/auth/**` open, everything else authenticated |
| **Kafka Publisher** | `KafkaPublisher` | Publishes task IDs to per-node Kafka topics (`tasks.node.<nodeId>`) |
| **Kafka Producer Config** | `KafkaProducerConfig` | Configures Kafka producer with idempotence, acks=all, retries |
| **Node Topic Provisioner** | `NodeTopicProvisioner` | On startup, creates Kafka topics for all active nodes |
| **Task Timeout Job** | `TaskTimeoutJob` | Scheduled cron (every minute): marks `in_progress` tasks as `timed_out` after 15 min |
| **Global Exception Handler** | `GlobalExceptionHandler` | Catches validation + generic exceptions, returns structured error responses |

### 2. Worker Node (`worker/`)

| Component | Class | Responsibility |
|---|---|---|
| **Task Consumer** | `TaskConsumer` | Kafka listener on `tasks.node.<nodeId>` topic; orchestrates fetch → execute → status update |
| **Task Executor** | `TaskExecutor` | Executes tasks by type: `EMAIL_SEND`, `PDF_GENERATE`, `DATA_EXPORT`, `REPORT_BUILD` |
| **Server API Client** | `ServerApiClient` | REST client to call server APIs with automatic JWT retry on 401 |
| **Token Manager** | `TokenManager` | Manages JWT lifecycle: login, cache token, auto-refresh before expiry |
| **Kafka Consumer Config** | `KafkaConsumerConfig` | Configures Kafka consumer with per-node group ID and topic binding |

### 3. MySQL Database (`node_tasks`)

Three tables: `node`, `task`, `user` — see [`schema.sql`](schema.sql) for DDL.

### 4. Apache Kafka

- **One topic per node**: `tasks.node.1`, `tasks.node.2`, etc.
- Topics are auto-created on server startup by `NodeTopicProvisioner`
- Each worker consumes only from its own topic
- Producer configured with `acks=all`, idempotence enabled, 5 retries

---

## Security Flow

```mermaid
sequenceDiagram
    participant Node as Worker Node
    participant Server as Main Server
    participant DB as MySQL

    Note over Node,Server: Authentication
    Node->>Server: POST /auth/login/node {nodeId, nodeSecret}
    Server->>DB: Find node by nodeId
    DB-->>Server: Node record (with BCrypt hash)
    Server->>Server: BCrypt.matches(secret, hash)
    Server->>Server: JwtService.mintNodeToken(nodeId)
    Server-->>Node: {accessToken, expiringOn}

    Note over Node,Server: Authorized API Call
    Node->>Server: GET /task/getAllForNode<br/>Authorization: Bearer <JWT>
    Server->>Server: JwtAuthFilter extracts nodeId from JWT
    Server->>Server: Sets NodeAuthentication in SecurityContext
    Server->>DB: SELECT * FROM task WHERE assigned_node_id = <nodeId>
    DB-->>Server: Task list
    Server-->>Node: [{taskId, taskType, ...}]

    Note over Node,Server: Ownership Enforcement
    Node->>Server: GET /task/getByTaskId?taskId=xxx
    Server->>Server: Extract nodeId from JWT
    Server->>DB: SELECT WHERE task_id=xxx AND assigned_node_id=<nodeId>
    alt Node owns the task
        Server-->>Node: Task details
    else Node does NOT own the task
        Server-->>Node: Empty result (access denied)
    end
```

---

## Task Lifecycle — State Machine

```mermaid
stateDiagram-v2
    [*] --> pending : Task Created
    pending --> in_progress : Worker picks up task
    in_progress --> completed : Execution succeeds
    in_progress --> failed : Execution throws exception
    in_progress --> timed_out : TaskTimeoutJob (15 min timeout)
    timed_out --> pending : Admin reassigns task
    failed --> pending : Admin reassigns task
    completed --> [*]

    note right of completed : Terminal state — cannot be changed
```

### Status Transition Rules

| From | Allowed To | Blocked |
|---|---|---|
| `pending` | `in_progress` | — |
| `in_progress` | `completed`, `failed` | `pending` |
| `completed` | **NONE** (terminal) | All transitions blocked |
| `failed` | `pending` (via reassign) | — |
| `timed_out` | `pending` (via reassign) | — |

---

## Failover & Recovery

```mermaid
flowchart LR
    A["Node Fails"] --> B{"Tasks stuck as<br/>in_progress"}
    B --> C["TaskTimeoutJob runs<br/>every 60 seconds"]
    C --> D["Tasks marked<br/>timed_out"]
    D --> E{"Admin Action"}
    E --> F["Reassign single task<br/>PUT /task/reassignTask"]
    E --> G["Reassign all tasks<br/>PUT /task/reassignAllForNode"]
    F --> H["Task re-published<br/>to new node's Kafka topic"]
    G --> H
    H --> I["New worker picks up<br/>and executes task"]
```

### Key Points

1. **Tasks remain locked**: When a node fails, its tasks stay assigned to it (no automatic steal).
2. **Timeout detection**: `TaskTimeoutJob` runs every minute. If an `in_progress` task hasn't been updated in 15 minutes (`app.task.timeoutSeconds=900`), it's marked `timed_out`.
3. **Manual/Admin reassignment**: Admin user can reassign a single task or all non-completed tasks of a node to a different node.
4. **Kafka re-notification**: On reassignment, the task ID is re-published to the new node's Kafka topic, so the new worker picks it up automatically.

---

## Data Flow — Task Creation to Completion

```mermaid
sequenceDiagram
    participant Admin
    participant Server
    participant DB as MySQL
    participant Kafka
    participant Worker as Worker Node

    Admin->>Server: POST /task/create<br/>{taskType, taskDetails, assignedNodeId}
    Server->>DB: Validate node exists
    Server->>DB: INSERT task (status=pending)
    Server->>Kafka: Publish taskId to tasks.node.<nodeId>
    Server-->>Admin: Task created (200 OK)

    Kafka-->>Worker: taskId delivered
    Worker->>Server: GET /task/getByTaskId?taskId=xxx
    Server-->>Worker: Task details

    Worker->>Server: PUT /task/updateStatus<br/>?taskId=xxx&oldStatus=pending&newStatus=in_progress
    Server->>DB: UPDATE task SET status=in_progress

    Worker->>Worker: TaskExecutor.execute(taskType, details)

    alt Success
        Worker->>Server: PUT /task/updateStatus<br/>?taskId=xxx&oldStatus=in_progress&newStatus=completed
        Server->>DB: UPDATE task SET status=completed
    else Failure
        Worker->>Server: PUT /task/updateStatus<br/>?taskId=xxx&oldStatus=in_progress&newStatus=failed
        Server->>DB: UPDATE task SET status=failed
    end
```
