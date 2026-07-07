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






---

# Phase 1: Monolith to Microservices Foundation

## Objective

The objective of Phase 1 was to decompose the existing Spring Boot Billing Management monolith into independent microservices while preserving business functionality and introducing production-oriented SaaS architecture concepts.

The focus of this phase was on establishing clear service boundaries, independent databases, synchronous inter-service communication, API key-based machine authentication, and usage metering.

---

# Milestone 1: Service Decomposition

## Objective

Separate the monolithic application into independent business domains.

## Deliverables

### 1. Billify Core Service

Created the primary business service responsible for managing billing-related operations.

Responsibilities:

* User Management
* Authentication & JWT Security
* Plans
* Subscriptions
* Payments
* Invoice Management

Database:

```text
core_db
```

---

### 2. Usage Service

Created an independent service responsible for usage metering.

Responsibilities:

* Usage Event Storage
* Usage Summary Aggregation

Database:

```text
usage_db
```

---

### 3. Independent Databases

Separated persistence according to microservice ownership.

```
Billify Core Service
        │
        └── core_db

Usage Service
        │
        └── usage_db
```

This established the principle that each microservice exclusively owns its database.

---

### 4. Independent Deployment

Configured both services as standalone Spring Boot applications capable of running independently.

---

### 5. Health Endpoints

Implemented health check endpoints to verify service availability.

---

# Milestone 2: Inter-Service Communication

## Objective

Enable synchronous communication between microservices.

---

### 1. Invoice Domain

Introduced the Invoice business domain.

Implemented:

* Invoice Entity
* Invoice Repository
* Invoice Service
* Invoice Controller
* Invoice DTOs

---

### 2. OpenFeign Integration

Configured declarative REST communication between services.

```
Billify Core Service
        │
        │ OpenFeign
        ▼
Usage Service
```

---

### 3. Usage Event Propagation

After successful invoice creation:

```
Invoice Created

↓

Usage Service notified

↓

Usage Event Stored
```

Implemented using OpenFeign client.

---

### 4. Transactional Invoice Flow

Invoice creation now performs:

1. Validate request
2. Save Invoice
3. Notify Usage Service
4. Return response

This became the first distributed business workflow.

---

# Milestone 3: SaaS Foundation

## Objective

Transform the billing application into a SaaS platform by introducing API-based access control, usage metering, and subscription quota enforcement.

---

## 1. Plan Enhancements

Extended subscription plans with usage quotas.

Added:

```java
invoiceLimit

apiCallLimit
```

Each subscription plan now defines:

* Maximum invoices
* Maximum API requests

---

## 2. API Key Management Domain

Introduced a dedicated API Key module.

Implemented:

```
ApiKey Entity

ApiKey Repository

ApiKey Service

ApiKey Controller
```

Responsibilities:

* API Key generation
* API Key hashing
* API Key validation
* API Key lifecycle management

---

## 3. Secure API Key Storage

API keys are never stored in plaintext.

Stored fields include:

* Key Identifier
* BCrypt Hash
* Active Status
* Creation Timestamp
* Last Used Timestamp

---

## 4. Automatic API Key Generation

Business logic was integrated into the subscription workflow.

```
User

↓

Subscribe to Plan

↓

Subscription Activated

↓

Generate API Key

↓

Return API Key Once
```

This models the behavior of commercial SaaS platforms.

---

## 5. API Key Protected Invoice API

Invoice creation now requires:

```
X-API-KEY
```

Validation flow:

```
Receive Request

↓

Extract API Key

↓

Validate Identifier

↓

Verify BCrypt Hash

↓

Resolve User

↓

Continue Invoice Processing
```

---

## 6. Usage Event Enhancement

Extended usage event model.

Added:

```java
apiKeyId
```

Every usage event is now associated with the API key responsible for generating it.

This enables:

* API usage analytics
* Key-level auditing
* Abuse detection
* Future rate limiting

---

## 7. Usage Summary Aggregation

Implemented materialized usage counters.

Created:

```
UsageSummary
```

Stores:

* Invoice Count
* API Call Count

Rather than calculating usage by scanning all events, counters are updated whenever a new event is recorded.

---

## 8. Usage Summary API

Implemented:

```
GET /api/usage/{userId}
```

Returns:

* Invoice Count
* API Call Count

This endpoint is used by the Core Service for quota validation.

---

## 9. Subscription Quota Enforcement

Before invoice creation:

```
Validate API Key

↓

Retrieve Usage Summary

↓

Retrieve Subscription

↓

Retrieve Plan

↓

Compare Usage Against Limits
```

If limits are exceeded:

```
Reject Request
```

Otherwise:

```
Create Invoice
```

---

## 10. End-to-End SaaS Request Flow

```
Application

        │
        │ X-API-KEY
        ▼

Billify Core Service

        │
        ├── Validate API Key
        │
        ├── Resolve User
        │
        ├── Retrieve Usage Summary
        │
        ├── Validate Plan Limits
        │
        ├── Create Invoice
        │
        └── OpenFeign Call
                │
                ▼

Usage Service

        │
        ├── Store Usage Event
        │
        └── Update Usage Summary
```

---

# Technologies Introduced in Phase 1

* Spring Boot 3
* Spring Security
* JWT Authentication
* Spring Data JPA
* OpenFeign
* MySQL (development)
* BCrypt API Key Hashing
* RESTful Microservices
* DTO-based Inter-service Communication
* Transaction Management
* Separate Databases per Service

---

# Architectural Concepts Learned

During Phase 1, the following microservice architecture concepts were implemented and understood:

* Monolith decomposition into bounded contexts
* Microservice service boundaries
* Database-per-service pattern
* Independent deployment model
* Synchronous inter-service communication using OpenFeign
* Domain-driven ownership of data
* API Key-based machine authentication
* Secure credential hashing
* SaaS subscription lifecycle
* Usage event logging
* Aggregated usage metering
* Subscription quota enforcement
* Service-to-service DTO contracts
* Distributed request flow

---

# Final Architecture After Phase 1

```text
                    Client Application
                           │
                           │ JWT / X-API-KEY
                           ▼
               +-------------------------+
               | Billify Core Service    |
               +-------------------------+
               | Users                   |
               | Authentication          |
               | Plans                   |
               | Subscriptions           |
               | Payments                |
               | API Keys                |
               | Invoices                |
               +-------------------------+
                    │
                    │ OpenFeign
                    ▼
               +-------------------------+
               | Usage Service           |
               +-------------------------+
               | Usage Events            |
               | Usage Summary           |
               +-------------------------+

            core_db                  usage_db
```


















---

# Phase 2 – Milestone 1

## Architectural Refinement & Production Readiness

### Objective

The objective of this milestone was not to introduce new infrastructure but to improve the quality of the existing microservice architecture before introducing service discovery, API Gateway, asynchronous messaging, and distributed tracing.

After Phase 1, Billify had already been decomposed into two independent microservices with separate databases and synchronous communication using OpenFeign. While functionally complete, several architectural improvements were required to align the codebase with Domain-Driven Design (DDD), Spring Boot best practices, JPA optimization techniques, and production-grade microservice design.

This milestone focused on strengthening domain modeling, reducing coupling, standardizing internal APIs, improving transaction management, and preparing the services for future infrastructure components such as Eureka and Spring Cloud Gateway.

---

# 1. Domain Model Refinement (JPA Entity Audit)

## Why this was necessary

In the initial implementation, the application was primarily focused on functionality. Relationships between entities were represented in some places using primitive foreign key fields such as:

```java
private Long userId;
```

Although functional, this approach prevents the ORM from understanding the actual relationships between domain objects.

It also limits navigation, reduces expressiveness of the domain model, and makes complex queries more difficult.

The objective was therefore to transform the persistence model into a richer object-oriented domain model.

---

## What was done

The entity model across the Billify Core Service was audited.

Primitive foreign key fields were replaced with proper entity associations wherever ownership naturally existed.

Examples include:

* Invoice → User
* ApiKey → User

using:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(...)
```

instead of storing only primitive IDs.

Hibernate now automatically manages foreign key relationships while preserving referential integrity at the database level.

---

## Why Lazy Loading?

Every relationship was intentionally configured with:

```java
FetchType.LAZY
```

instead of eager loading.

This ensures related entities are fetched only when actually required.

Instead of executing:

```text
Invoice

↓

User

↓

Subscription

↓

Plan
```

for every invoice retrieval, Hibernate loads only the Invoice object initially.

Associated entities are retrieved only if business logic explicitly accesses them.

This significantly reduces unnecessary database operations and memory usage.

---

## Aggregate Boundary Review

Another important improvement was reducing unnecessary bidirectional relationships.

For example:

Instead of

```text
User

↓

Invoices

↓

User

↓

Invoices
```

creating circular object graphs,

the User aggregate no longer directly maintains collections of Invoice or ApiKey entities.

Each aggregate now owns only the relationships required to enforce its own business rules.

This reduces coupling, minimizes serialization problems, and aligns the domain model with Domain-Driven Design principles.

---

# 2. Package Structure Standardization

## Why this was necessary

Phase 1 focused primarily on implementing business functionality.

As the project grows into multiple microservices, inconsistent package organization becomes increasingly difficult to maintain.

Each service should follow the same architectural conventions so that developers can navigate the codebase consistently.

---

## What was done

Both the Billify Core Service and the Usage Service were reorganized into standardized package structures.

Each service now contains clearly separated layers such as:

```text
controller

service

repository

entity

dto

mapper

client

security

config

exception
```

This improves readability, maintainability, and scalability.

A developer joining the project can immediately locate responsibilities regardless of which microservice they are working on.

---

# 3. Separation of DTO Layers

## Problem

Initially, DTOs were beginning to serve multiple purposes.

A single DTO might have been used:

* for REST requests,
* REST responses,
* and Feign communication.

This creates coupling between external APIs and internal service contracts.

---

## Improvement

DTOs were divided into separate categories.

External API DTOs represent communication with frontend clients.

Internal DTOs represent service-to-service communication.

This creates a clear architectural boundary:

```text
Client

↓

REST DTO

↓

Business Layer

↓

Feign DTO

↓

Remote Service
```

Each layer now owns its own contract.

This significantly improves maintainability because internal service communication can evolve independently from public APIs.

---

# 4. Service Responsibility Refinement

One important architectural objective was reducing responsibility concentration.

For example, invoice creation originally contained:

* validation
* quota verification
* persistence
* OpenFeign communication

inside a single service flow.

To improve separation of concerns, persistence responsibilities were extracted into a dedicated component:

```text
InvoicePersistenceService
```

This allows:

InvoiceService

to focus primarily on orchestration,

while

InvoicePersistenceService

handles persistence-specific responsibilities.

This follows the Single Responsibility Principle and makes unit testing considerably easier.

---

# 5. Transaction Boundary Optimization

This is arguably one of the most important backend improvements introduced in this milestone.

## Original problem

The invoice creation process performed:

```text
Start Transaction

↓

Validate API Key

↓

OpenFeign Call

↓

Save Invoice

↓

Commit
```

Holding a database transaction open while waiting for another microservice introduces several problems:

* long-running database locks
* reduced throughput
* increased contention
* poor scalability
* distributed transaction risks

---

## Improvement

The transaction scope was redesigned.

Validation and remote service communication now occur outside the transactional context.

Only local persistence operations remain transactional.

The new flow becomes:

```text
Validate API Key

↓

Retrieve Usage Summary

↓

Business Validation

↓

Open Transaction

↓

Persist Invoice

↓

Commit
```

The database transaction now remains open only for the minimum amount of time required.

This is a standard production optimization used in enterprise applications.

---

# 6. Query Optimization (N+1 Prevention)

JPA applications frequently suffer from the N+1 query problem.

Instead of executing:

```text
1 query

+

100 additional queries
```

for related objects,

optimized repository queries now explicitly use:

```sql
JOIN FETCH
```

This loads required associations in a single SQL statement.

Benefits include:

* fewer database round trips
* improved response time
* lower database load

This becomes increasingly important as application traffic grows.

---

# 7. Internal vs External API Separation

Initially, all REST endpoints appeared similar regardless of whether they were intended for frontend clients or other microservices.

This creates ambiguity and unnecessary exposure.

The architecture now clearly distinguishes:

Public APIs:

```text
/api/*
```

used by external clients,

and

Internal APIs:

```text
/internal/*
```

used exclusively for service-to-service communication.

For example:

```text
Client

↓

POST /api/invoices
```

versus

```text
Core Service

↓

GET /internal/usage/{userId}
```

This prepares the architecture for API Gateway integration, where only public endpoints will be exposed externally.

---

# 8. Standardized Error Handling

Previously, exceptions could produce inconsistent responses across services.

A unified error response model was introduced.

Every failure now returns a consistent structure containing:

* timestamp
* HTTP status
* error code
* message
* request path
* additional details

Benefits include:

* easier frontend integration
* predictable API contracts
* improved debugging
* centralized exception management

The `GlobalExceptionHandler` ensures domain-specific exceptions are automatically translated into meaningful HTTP responses.

---

# 9. Configuration Externalization

Hardcoded assumptions such as service URLs were removed.

Instead of embedding:

```text
localhost:8081
```

throughout the application,

configuration now uses property placeholders such as:

```properties
services.usage-service.url
```

This provides environment independence.

Today the value points to localhost.

After introducing Eureka, the same configuration can simply become:

```properties
http://billify-usage-service
```

without changing business code.

This is an important step toward cloud-native architecture.

---

# 10. Production Readiness Verification

The milestone concluded with a comprehensive verification process.

Both services were successfully compiled and tested.

Special attention was given to validating the invoice creation workflow, ensuring that:

* valid API keys successfully create invoices,
* usage events are propagated correctly,
* quota enforcement prevents over-consumption,
* failed quota validation avoids unnecessary persistence,
* Feign communication behaves as expected.

The introduction of automated tests significantly increases confidence when future infrastructure changes—such as Eureka, API Gateway, Redis, and RabbitMQ—are added.

---

# Overall Architectural Outcome

Before this milestone, Billify consisted of two functional microservices communicating synchronously via OpenFeign. While operational, the architecture still contained characteristics commonly found in applications transitioning from a monolith, such as tightly scoped service responsibilities, implicit entity relationships, and infrastructure-specific assumptions.

After completing Phase 2 Milestone 1, the architecture has been transformed into a significantly more mature and maintainable system. The domain model now accurately represents business relationships through well-defined JPA mappings, aggregate boundaries are clearer, service responsibilities are better separated, transaction scopes are optimized, DTOs and API contracts are explicitly categorized, internal and external APIs are distinguished, exception handling is standardized, and configuration has been externalized in preparation for service discovery.

This milestone establishes a robust architectural foundation that allows subsequent infrastructure enhancements—such as **Eureka Service Discovery**, **Spring Cloud Gateway**, **Redis**, **RabbitMQ**, and **distributed tracing**—to be introduced incrementally without requiring major refactoring of the existing business logic. In essence, the focus shifted from simply building a working microservice application to engineering a clean, scalable, and production-ready microservice architecture.







# Phase 2: Milestone 2 Plan:
# Milestone 2 Deliverables:

## Deliverable 1 — Eureka Server

Create:

```text
eureka-server
```

Responsibilities:

* Registry
* Dashboard
* Instance Discovery

Nothing else.

---

## Deliverable 2 — Register Existing Services

Current:

```text
Core

Usage
```

Both register themselves.

Dashboard should show:

```text
Applications

BILLIFY-CORE-SERVICE

UP

USAGE-SERVICE

UP
```

---

## Deliverable 3 — Replace Static URLs

Current:

```properties
services.usage-service.url=http://localhost:8081
```

Becomes:

```java
@FeignClient(name = "usage-service")
```

No URLs.

No ports.

No localhost.

---

## Deliverable 4 — Client-Side Load Balancing

This is something I definitely want you to learn.

Suppose:

```text
Usage #1

Usage #2

Usage #3
```

Feign should automatically distribute requests.

You don't even have to write load balancing logic.

We'll learn how Spring Cloud LoadBalancer works internally.

---

## Deliverable 5 — Instance Failure

Kill one Usage Service instance.

Observe:

```text
Usage #2

DOWN
```

Core continues working.

This demonstrates why service discovery exists.

---

## Deliverable 6 — Service Registration Lifecycle

Understand:

```text
Start Service

↓

Register

↓

Heartbeat

↓

Healthy

↓

Shutdown

↓

Deregister
```

Most tutorials never explain this lifecycle.

We'll spend time understanding it.

---

## Deliverable 7 — Configuration Cleanup

Your configuration becomes dramatically simpler.

Instead of:

```yaml
services:
  usage-service:
    url: http://localhost:8081
```

you'll only configure:

```yaml
spring:
  application:
    name: billify-core-service
```

and Eureka takes over service location.

---

# One Architectural Improvement I Want to Add

This wasn't in the original roadmap, but I think it would make Billify significantly stronger.

Currently your Feign communication is:

```text
Core

↓

Usage
```

If Usage is down:

```text
FeignException
```

What happens?

Right now, invoice creation probably fails.

Before we add Gateway, I want to introduce **Resilience**.

Not necessarily Resilience4j immediately, but at least discuss and possibly implement:

* Connection timeouts
* Read timeouts
* Retry policy (only where safe)
* Fallback strategy
* Graceful degradation
* Idempotency considerations

These topics naturally follow service discovery because distributed systems are not only about *finding* services—they're also about handling the reality that services sometimes fail.

---

# My Proposed Phase 2 Milestone 2

I would define it as:

1. **Introduce Eureka Server**
2. **Register Core and Usage services**
3. **Replace static Feign URLs with service discovery**
4. **Understand client-side load balancing**
5. **Understand registration, heartbeat, and deregistration**
6. **Run multiple Usage Service instances and observe load balancing**
7. **Introduce basic resilience concepts (timeouts, retries, graceful failure)**
8. **Verify end-to-end request flow through Eureka**

---


