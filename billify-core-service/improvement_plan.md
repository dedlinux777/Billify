# Big Architectural Picture

Imagine a company called "Acme CRM" wants to use Billify.

Acme CRM purchases a plan from Billify.

Billify gives them:

* Subscription management
* Billing
* API keys
* Usage tracking
* Quota enforcement
* Alerts

Now Acme CRM's application can communicate with Billify APIs.

The architecture becomes:

```text
                          ┌─────────────────────┐
                          │     React Admin     │
                          │      Dashboard      │
                          └──────────┬──────────┘
                                     │
                                     ▼
                       ┌───────────────────────────┐
                       │       API Gateway         │
                       │ Auth + Routing + Logging  │
                       └───────┬─────────┬─────────┘
                               │         │
               ┌───────────────┘         └───────────────┐
               ▼                                         ▼

    ┌─────────────────────┐                 ┌─────────────────────┐
    │   Billify Core      │                 │   Usage Service     │
    │                     │◄────REST──────►│                     │
    │ Users               │                 │ Usage Metering      │
    │ Plans               │                 │ Quota Checks        │
    │ Subscriptions       │                 │ API Analytics       │
    │ Invoices            │                 │ Redis Counters      │
    │ API Keys            │                 └─────────┬───────────┘
    └──────────┬──────────┘                           │
               │                                      │
               │ Events                               │ Events
               ▼                                      ▼

                    ┌─────────────────────────┐
                    │      RabbitMQ           │
                    │   Event Bus/Broker      │
                    └──────────┬──────────────┘
                               │
                               ▼

                  ┌───────────────────────────┐
                  │ Notification Service      │
                  │ Email Alerts              │
                  │ Usage Warnings            │
                  │ Payment Alerts            │
                  │ Renewal Reminders         │
                  └───────────────────────────┘


                    ┌─────────────────────┐
                    │       Redis         │
                    │ Usage Counters      │
                    │ Fast Lookups        │
                    └─────────────────────┘
```

---

# What Business Problem Does Each Service Solve?

This is extremely important for interviews.

Every microservice must answer:

> "Why does this service exist?"

---

## Billify Core Service

This is the brain of the system.

### Responsibilities:

```text
User Management
Authentication
Subscription Plans
Invoices
Payments
API Keys
```

It owns business data.

Example:

```text
User Harsha

Plan:
Professional

Monthly Invoice Limit:
1000

API Request Limit:
10000
```

All of this lives here.

---

## Usage Metering Service

This is the most interesting service.

Real SaaS products don't just ask:

```text
Who is subscribed?
```

They ask:

```text
How much has the customer consumed?
```

Examples:

AWS:

```text
500GB Storage
20000 API Calls
```

Stripe:

```text
2500 Transactions
```

Twilio:

```text
10000 SMS
```

Billify:

```text
Invoices Generated
API Calls Consumed
```

The Usage Service tracks these numbers.

---

# Why Redis Lives Here

Because usage tracking is extremely write-heavy.

Imagine:

```text
10000 API requests
```

If every request writes to PostgreSQL:

```text
10000 DB Writes
```

Not ideal.

Redis is designed exactly for this.

Instead:

```java
jedis.incr("usage:user42")
```

takes microseconds.

---

# Why Not Store Usage in Core Service?

Because:

```text
Core Service = Business Data

Usage Service = Operational Metrics
```

Separating them is a common microservice pattern.

---

# RabbitMQ's Role

RabbitMQ exists because services shouldn't know who listens to them.

Without RabbitMQ:

```text
Usage Service
      │
      └──► Notification Service
```

Direct dependency.

With RabbitMQ:

```text
Usage Service
      │
      ▼
   RabbitMQ
      │
      ▼
Notification Service
```

Usage Service simply says:

```json
{
  "event":"LIMIT_REACHED"
}
```

and doesn't care who receives it.

---

# Event Flow Example

Imagine Harsha has:

```text
Invoice Limit = 100
```

Current Usage:

```text
79
```

---

User clicks:

```text
Generate Invoice
```

Frontend calls:

```http
POST /invoice
```

---

Billify Core:

```text
Creates Invoice
```

then informs:

```text
Usage Service
```

---

Usage Service:

```text
79 → 80
```

Redis updated.

Usage percentage:

```text
80%
```

---

Usage Service publishes:

```json
{
  "event":"USAGE_LIMIT_80_PERCENT_REACHED",
  "userId":42
}
```

to RabbitMQ.

---

Notification Service receives event.

Sends email:

```text
Warning:
You have consumed 80% of your monthly quota.
```

---

No direct communication between services.

Very clean architecture.

---

# Detailed Technical Architecture

Now let's design the actual stack.

---

## Frontend

Purpose:

```text
Admin Dashboard
User Dashboard
Usage Analytics
Invoices
Subscriptions
```

Stack:

```text
React
Vite
Axios
React Router
Tailwind CSS
```

Optional:

```text
Recharts
```

for usage graphs.

---

## API Gateway

Purpose:

```text
Single Entry Point
Authentication
Routing
Rate Limiting
```

Stack:

```text
Spring Cloud Gateway
Spring Security
JWT Validation
```

Responsibilities:

```text
/api/auth/**
→ Core Service

/api/usage/**
→ Usage Service

/api/notifications/**
→ Notification Service
```

---

## Billify Core Service

Purpose:

```text
Business Domain
```

Stack:

```text
Spring Boot
Spring Security
Spring Data JPA
PostgreSQL
Lombok
Validation
```

Tables:

```text
users
plans
subscriptions
invoices
payments
api_keys
```

---

## Usage Metering Service

Purpose:

```text
Usage Tracking
Quota Validation
Analytics
```

Stack:

```text
Spring Boot
Redis
Jedis
OpenFeign
```

Redis Keys:

```text
usage:user:42:invoice
usage:user:42:api
```

Features:

```text
Counter Increment
Plan Validation
Usage Analytics
Limit Enforcement
```

---

## Why OpenFeign?

Instead of manually writing:

```java
RestTemplate
WebClient
```

Use:

```java
@FeignClient("billify-core")
```

Interviewers love seeing this.

Example:

```java
@FeignClient(name="billify-core")
public interface PlanClient {

    @GetMapping("/internal/plan/{userId}")
    PlanResponse getPlan(Long userId);

}
```

Cleaner microservice communication.

---

## Notification Service

Purpose:

```text
Background Worker
```

Stack:

```text
Spring Boot
RabbitMQ
Java Mail Sender
```

Consumes:

```text
LIMIT_REACHED
PAYMENT_FAILED
SUBSCRIPTION_RENEWED
```

Produces:

```text
Emails
Webhooks
Slack Alerts
```

(optional)

---

## RabbitMQ

Purpose:

```text
Asynchronous Messaging
```

Topics:

```text
usage.events
billing.events
notification.events
```

Events:

```json
{
  "event":"PAYMENT_FAILED"
}
```

```json
{
  "event":"SUBSCRIPTION_RENEWED"
}
```

```json
{
  "event":"LIMIT_REACHED"
}
```

---

## Redis

Purpose:

```text
Fast Counters
```

Examples:

```text
usage:user:42:invoice
= 56
```

```text
usage:user:42:api
= 1200
```

Operations:

```java
jedis.incr()
jedis.get()
jedis.expire()
```

---

