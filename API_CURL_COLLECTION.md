# API cURL Collection — Distributed Task System

> **Base URL**: `http://localhost:8080`  
> Replace `<TOKEN>` with the JWT obtained from the login endpoints.

---

## 1. Authentication

### 1.1 Admin/User Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "kartik.roy@explorisin",
    "password": "muopshi"
  }'
```

**Response:**
```json
{
  "accessToken": "<ADMIN_JWT_TOKEN>",
  "role": "admin"
}
```

---

### 1.2 Node Login (Node 1)

```bash
curl -X POST http://localhost:8080/auth/login/node \
  -H "Content-Type: application/json" \
  -d '{
    "nodeId": "1",
    "nodeSecret": "node1Secret"
  }'
```

**Response:**
```json
{
  "accessToken": "<NODE_JWT_TOKEN>",
  "expiringOn": "2026-02-15T02:52:17"
}
```

---

### 1.3 Node Login (Node 2)

```bash
curl -X POST http://localhost:8080/auth/login/node \
  -H "Content-Type: application/json" \
  -d '{
    "nodeId": "2",
    "nodeSecret": "node2Secret"
  }'
```

---

### 1.4 Node Login (Node 3)

```bash
curl -X POST http://localhost:8080/auth/login/node \
  -H "Content-Type: application/json" \
  -d '{
    "nodeId": "3",
    "nodeSecret": "node2Secret"
  }'
```

---

### 1.5 Node Login (Node 4)

```bash
curl -X POST http://localhost:8080/auth/login/node \
  -H "Content-Type: application/json" \
  -d '{
    "nodeId": "4",
    "nodeSecret": "node1Secret"
  }'
```

---

## 2. Task Operations (Admin — requires User/Admin JWT)

### 2.1 Create Task — DATA_EXPORT

```bash
curl -X POST http://localhost:8080/task/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "taskType": "DATA_EXPORT",
    "taskDetails": "comp1",
    "assignedNodeId": "3"
  }'
```

**Response:**
```json
{
  "taskId": "07ab191c-057d-4331-a3a4-e6246f4ff2650",
  "taskType": "DATA_EXPORT",
  "taskDetails": "comp1",
  "assignedNodeId": "3",
  "status": "pending",
  "createdOn": "2026-02-15T01:12:17.666050",
  "updatedOn": "2026-02-15T01:12:17.666050"
}
```

---

### 2.2 Create Task — EMAIL_SEND

```bash
curl -X POST http://localhost:8080/task/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "taskType": "EMAIL_SEND",
    "taskDetails": "demo_lib",
    "assignedNodeId": "4"
  }'
```

---

### 2.3 Create Task — PDF_GENERATE

```bash
curl -X POST http://localhost:8080/task/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "taskType": "PDF_GENERATE",
    "taskDetails": "demo_report",
    "assignedNodeId": "4"
  }'
```

---

### 2.4 Reassign Single Task to Different Node

```bash
curl -X PUT "http://localhost:8080/task/reassignTask?taskId=07ab191c-057d-4331-a3a4-e6246f4ff2650&nodeId=2" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Response:**
```json
{
  "taskId": "07ab191c-057d-4331-a3a4-e6246f4ff2650",
  "taskType": "DATA_EXPORT",
  "taskDetails": "comp1",
  "assignedNodeId": "2",
  "status": "pending",
  "createdOn": "2026-02-15T01:12:17.666050",
  "updatedOn": "2026-02-15T01:23:12.654920"
}
```

---

### 2.5 Reassign All Tasks from One Node to Another

```bash
curl -X PUT "http://localhost:8080/task/reassignAllForNode?oldNodeId=3&newNodeId=4" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Response:**
```
OK
```

---

## 3. Task Operations (Node — requires Node JWT)

### 3.1 Get Task by Task ID (Node-scoped)

> Only returns the task if it is assigned to the requesting node.

```bash
curl -X GET "http://localhost:8080/task/getByTaskId?taskId=07ab191c-057d-4331-a3a4-e6246f4ff2650" \
  -H "Authorization: Bearer <NODE_TOKEN>"
```

**Response (task belongs to this node):**
```json
{
  "taskId": "07ab191c-057d-4331-a3a4-e6246f4ff2650",
  "taskType": "DATA_EXPORT",
  "taskDetails": "comp1",
  "assignedNodeId": "3",
  "status": "pending"
}
```

**Response (task does NOT belong to this node):**
```json
{
  "taskId": null,
  "taskType": null,
  "taskDetails": null,
  "assignedNodeId": null,
  "status": null
}
```

---

### 3.2 Get All Tasks for This Node

```bash
curl -X GET http://localhost:8080/task/getAllForNode \
  -H "Authorization: Bearer <NODE_TOKEN>"
```

**Response:**
```json
[
  {
    "taskId": "07ab191c-057d-4331-a3a4-e6246f4ff2650",
    "taskType": "DATA_EXPORT",
    "taskDetails": "comp1",
    "assignedNodeId": "3",
    "status": "pending"
  },
  {
    "taskId": "0e63210b-f460-4661-92a4-d37a6e36071d",
    "taskType": "DATA_EXPORT",
    "taskDetails": "comp1",
    "assignedNodeId": "3",
    "status": "completed"
  }
]
```

---

### 3.3 Update Task Status — pending → in_progress

```bash
curl -X PUT "http://localhost:8080/task/updateStatus?taskId=07ab191c-057d-4331-a3a4-e6246f4ff2650&oldStatus=pending&newStatus=in_progress" \
  -H "Authorization: Bearer <NODE_TOKEN>"
```

**Response:**
```json
{
  "taskId": "07ab191c-057d-4331-a3a4-e6246f4ff2650",
  "taskType": "DATA_EXPORT",
  "taskDetails": "comp1",
  "assignedNodeId": "3",
  "status": "in_progress",
  "createdOn": "2026-02-15T01:12:17.666050",
  "updatedOn": "2026-02-15T01:15:30.123456"
}
```

---

### 3.4 Update Task Status — in_progress → completed

```bash
curl -X PUT "http://localhost:8080/task/updateStatus?taskId=07ab191c-057d-4331-a3a4-e6246f4ff2650&oldStatus=in_progress&newStatus=completed" \
  -H "Authorization: Bearer <NODE_TOKEN>"
```

---

### 3.5 Update Task Status — in_progress → failed

```bash
curl -X PUT "http://localhost:8080/task/updateStatus?taskId=07ab191c-057d-4331-a3a4-e6246f4ff2650&oldStatus=in_progress&newStatus=failed" \
  -H "Authorization: Bearer <NODE_TOKEN>"
```

---

### 3.6 Attempt Invalid Status Update — completed → in_progress (BLOCKED)

```bash
curl -X PUT "http://localhost:8080/task/updateStatus?taskId=07ab191c-057d-4331-a3a4-e6246f4ff2650&oldStatus=completed&newStatus=in_progress" \
  -H "Authorization: Bearer <NODE_TOKEN>"
```

**Response:**
```
Invalid status update
```

---

## 4. Error Scenarios

### 4.1 No Authorization Header

```bash
curl -X GET http://localhost:8080/task/getAllForNode
```

**Response:** `401 Unauthorized`

---

### 4.2 Expired / Invalid Token

```bash
curl -X GET http://localhost:8080/task/getAllForNode \
  -H "Authorization: Bearer invalid.token.here"
```

**Response:** `401 Unauthorized`

---

### 4.3 Create Task with Non-existent Node

```bash
curl -X POST http://localhost:8080/task/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "taskType": "DATA_EXPORT",
    "taskDetails": "test",
    "assignedNodeId": "999"
  }'
```

**Response:** `400 Bad Request — "Node does not exist"`

---

## Quick Test Flow

```bash
# Step 1: Login as admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"kartik.roy@explorisin","password":"muopshi"}' | jq -r '.accessToken')

# Step 2: Create a task assigned to node 3
TASK=$(curl -s -X POST http://localhost:8080/task/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"taskType":"DATA_EXPORT","taskDetails":"quick_test","assignedNodeId":"3"}')
TASK_ID=$(echo $TASK | jq -r '.taskId')
echo "Created task: $TASK_ID"

# Step 3: Login as node 3
NODE_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login/node \
  -H "Content-Type: application/json" \
  -d '{"nodeId":"3","nodeSecret":"node2Secret"}' | jq -r '.accessToken')

# Step 4: Fetch tasks for node 3
curl -s -X GET http://localhost:8080/task/getAllForNode \
  -H "Authorization: Bearer $NODE_TOKEN" | jq .

# Step 5: Update status to in_progress
curl -s -X PUT "http://localhost:8080/task/updateStatus?taskId=$TASK_ID&oldStatus=pending&newStatus=in_progress" \
  -H "Authorization: Bearer $NODE_TOKEN" | jq .

# Step 6: Mark as completed
curl -s -X PUT "http://localhost:8080/task/updateStatus?taskId=$TASK_ID&oldStatus=in_progress&newStatus=completed" \
  -H "Authorization: Bearer $NODE_TOKEN" | jq .
```
