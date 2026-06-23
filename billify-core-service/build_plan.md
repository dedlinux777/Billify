# Plan:
Phase 1

Convert the monolith into only:

billify-core-service
usage-service

Communication:

OpenFeign

Database:

PostgreSQL

No RabbitMQ yet.
No Redis yet.

Learn:

Service boundaries
Separate databases
Feign communication
DTO sharing
Inter-service calls

Phase 2

Add:

Eureka

Learn:

Service discovery
Dynamic service registration
Load balancing

Phase 3

Add:

API Gateway

Learn:

Routing
JWT validation
Centralized entry point

Phase 4

Add:

Redis

Move usage counters into Redis.

Learn:

Cache vs database
Counters
Fast reads/writes

Phase 5

Add:

RabbitMQ

Learn:

Event-driven architecture
Async communication

Phase 6

Add:

Notification Service

Learn:

Event consumers
Background processing

Phase 7

Add:

Zipkin
Micrometer

Learn:

Distributed tracing

Phase 8

Add:

Docker Compose

Learn:

Containerized microservices


# Implementation
## Before Microservices

Original Monolith

```text id="xw4w1j"
Billify Monolith

├── Auth
├── Users
├── Plans
├── Subscriptions
├── Payments
```

Characteristics:

```text id="cfjjlwm"
Single Spring Boot Application

Single Database

Single Deployment

No Service Boundaries

No Inter-Service Communication
```

Everything lived together.

---

## Phase 1

Goal:

```text id="z0pqkz"
Convert Monolith

↓

Microservice Architecture Foundation
```

---

### Phase 1 — Milestone 1 Delivered

Goal:

```text id="5bnm4n"
Create independent services
```

Delivered:

```text id="l80c4s"
billify-core-service

usage-service
```

Databases:

```text id="n54tjq"
core_db

usage_db
```

New Domain:

```text id="7f1kjf"
UsageEvent

UsageSummary
```

Achievements:

```text id="vzt3yz"
Independent Deployments

Independent Databases

Health Endpoints

Service Ownership Established
```

Architecture:

```text id="v5ex4n"
Core Service
```

owns:

```text id="f6x2s4"
Users
Plans
Subscriptions
Payments
```

Usage Service owns:

```text id="kgwvxz"
Usage Events
Usage Summaries
```

---

### Phase 1 — Milestone 2 Delivered

Goal:

```text id="pp79cg"
Microservice Communication
```

Delivered:

```text id="8h7qaz"
Invoice Domain

OpenFeign Integration
```

New Core Domain:

```text id="kr51a7"
Invoice
InvoiceRepository
InvoiceService
InvoiceController
```

New Communication:

```text id="u2y23j"
Core Service

↓

OpenFeign

↓

Usage Service
```

Workflow:

```text id="umh74j"
POST /api/invoices

↓

Invoice Created

↓

Usage Event Created

↓

201 Created
```

Achievements:

```text id="dbcz2q"
Service-to-Service Communication

DTO Contracts

Feign Clients

Distributed Workflow
```

This was your first real microservice business flow.

---

### Phase 1 Milestone 3

Milestone 3 becomes:

```text
Subscription
        |
        v
API Key
        |
        v
Invoice API
        |
        v
Usage Tracking
        |
        v
Usage Summary
```

This is much closer to Stripe/OpenAI/Twilio.

---

#### Deliverable 1

##### Plan Domain Enhancement

Current Plan probably contains:

```java
name

price

duration
```

We should add:

```java
invoiceLimit

apiCallLimit
```

Example:

```java
Starter

invoiceLimit = 100

apiCallLimit = 1000
```

```java
Professional

invoiceLimit = 1000

apiCallLimit = 10000
```

Now plans become quota-aware.

---

#### Deliverable 2

##### API Key Domain

New package:

```text
apikey

├── ApiKey
├── ApiKeyRepository
├── ApiKeyService
├── ApiKeyController
```

Entity:

```java
ApiKey

id

userId

keyIdentifier

keyHash

active

createdAt

lastUsedAt
```

Notice:

```java
keyHash
```

not:

```java
apiKey
```

We never store the secret.

---

#### Deliverable 3

##### Subscription → API Key Business Logic

This is the biggest design change.

Question:

When should API Keys be created?

Option A

```text
User manually generates key
```

Option B

```text
Subscription becomes active

↓

API Key automatically generated
```

For Billify I recommend:

```text
Subscription Activated

↓

Generate Default API Key
```

Why?

Because:

```text
User buys API platform

↓

User should immediately receive API access
```

No extra step.

This is how many SaaS products behave.

---

#### New Subscription Flow

Current:

```text
User

↓

Subscription

Done
```

Future:

```text
User

↓

Subscription Activated

↓

Generate API Key

↓

Return Key To User
```

---

#### Deliverable 4

##### API Key Validation Layer

Currently:

```http
POST /api/invoices
```

only relies on JWT.

Milestone 3 introduces:

```http
X-API-KEY:
bk_live_xxxxx
```

Flow:

```text
Invoice Request

↓

Extract API Key

↓

Validate Key

↓

Find User

↓

Continue
```

---

Question:

Should JWT still exist?

My recommendation:

Keep JWT.

Reason:

```text
JWT

=
Dashboard User
```

```text
API Key

=
Machine/Application
```

Exactly how GitHub works.

---

#### Deliverable 5

##### UsageEvent Enhancement

Current:

```java
UsageEvent

id

userId

resourceType

createdAt
```

Future:

```java
UsageEvent

id

userId

apiKeyId

resourceType

createdAt
```

This is extremely important.

Now Usage Service can answer:

```text
Which API key generated usage?
```

```text
Which API key is abused?
```

```text
Which API key generated 90% traffic?
```

---

#### Deliverable 6

#### UsageSummary Implementation

Now we finally implement it.

Current:

```text
UsageEvent only
```

Future:

```text
UsageEvent
+
UsageSummary
```

---

UsageSummary

```java
id

userId

invoiceCount

apiCallCount
```

---

When Usage Service receives:

```json
{
   "userId":1,
   "apiKeyId":5,
   "resourceType":"INVOICE"
}
```

it should:

Step 1

```text
Save UsageEvent
```

Step 2

```text
Increment invoiceCount
```

in UsageSummary.

---

#### Important Design Decision

Should invoice generation increment:

```text
invoiceCount only
```

or

```text
invoiceCount
+
apiCallCount
```

My recommendation:

Increment both.

Reason:

Customer consumed:

```text
Invoice Resource
```

and also made:

```text
API Request
```

So Usage Service should create:

```text
UsageEvent(INVOICE)
```

and update:

```text
invoiceCount++

apiCallCount++
```

This gives us multi-dimensional metering.

---

#### Final Architecture After Milestone 3

```text
User

  |
  | Subscription Purchase
  v

Core Service

  |
  | Generate API Key
  v

ApiKey Table

------------------------------------------------

Application

  |
  | X-API-KEY
  v

POST /api/invoices

  |
  | Validate API Key
  |
  | Create Invoice
  |
  | Feign
  v

Usage Service

  |
  | Save UsageEvent
  |
  | Update UsageSummary
  v

usage_db
```

This is the first point where Billify starts resembling a commercial SaaS platform.
