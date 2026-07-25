## 🏗️ System Overview & Architectural Guardrails

Before tracing specific user actions, it is essential to understand the cross-cutting mechanics handling every request entering the platform:

```text
[ React Frontend / External B2B Client ]
                    │
                    ▼
          ┌───────────────────┐
          │    API Gateway    │ (Port 8080)
          [cite_start]│  (Cloud Gateway)  │ ── Generates Correlation ID & Validates JWT [cite: 10, 15, 19]
          └─────────┬─────────┘
                    │
   ┌────────────────┼────────────────┐
   │ (Internal)     │                │
   ▼                ▼                ▼
┌──────────────────────┐ ┌──────────────────────────┐ ┌──────────────────────┐
│ billify-core-service │ │ saas-execution-service   │ │    usage-service     │
│ (Users, Plans, Sub)  │ │ (Invoices, Quota Check)  │ │  (Usage Event Logs)  │
└──────────────────────┘ └──────────────────────────┘ └──────────────────────┘

```

1.
**Edge Correlation ID Generation**: Every incoming HTTP request hits `CorrelationIdFilter` in `api-gateway`. If no `X-Correlation-ID` header exists, a UUID is generated and injected into request and response headers.


2.
**Inter-Service Correlation Propagation**: When a microservice makes an internal call via OpenFeign (e.g., `saas-execution-service` calling `usage-service`), `FeignCorrelationInterceptor` grabs the `X-Correlation-ID` from the current thread's MDC log context and attaches it to outgoing Feign request headers.


3.
**Edge Security & Downstream Trust**: `api-gateway` validates incoming Bearer JWTs using `JwtAuthenticationFilter`. Once validated, it injects `X-User-Email` and `X-User-Role` headers. Downstream services read these trusted headers in their local `JwtAuthFilter` without needing to re-parse cryptographic token signatures.


4.
**Internal Network Perimeter**: Internal APIs (`/internal/**`) are **not** mapped in `application.yml` of `api-gateway`. Any direct external request attempting to reach internal service routes through the Gateway will be blocked with a `404 Not Found`.



---

## 👤 Perspective 1: Unauthenticated & Standard End-User Workflows

### Workflow 1: User Registration & Login

**Goal:** User registers or logs in to obtain a stateless JWT token.

```text
[Frontend] ──> POST /api/auth/login ──> [API Gateway] ──> [billify-core-service]

```

1.
**Frontend Request**: The React application submits user credentials (`email`, `password`) via `POST /api/auth/login`.


2. **API Gateway Processing**:
* Matches route `core-auth-route` (`/api/auth/**`) targeting `lb://billify-core-service`.


*
`CorrelationIdFilter` stamps request with `X-Correlation-ID`.


* Bypasses `JwtAuthenticationFilter` because `/api/auth/**` is public.




3. **billify-core-service Processing**:
* Handled by `AuthController.login()` -> `AuthService.login()`.


* Validates password hash against `UserRepository`.


* Generates a JWT token embedded with `subject: email` and claim `role: ROLE_USER`.




4.
**Response**: Returns `{ "token": "<JWT_STRING>" }`. Frontend saves it in `localStorage`.



---

### Workflow 2: End-User Subscribes to a Plan

**Goal:** Authenticated user views available plans and subscribes to one.

```text
[Frontend] ──> POST /api/subscriptions/subscribe/{planId} (Bearer Token)
                     │
                     ▼
             [API Gateway] (Validates JWT -> Injects X-User-Email & X-User-Role)
                     │
                     ▼
         [billify-core-service] (Executes @Transactional Subscribe & Generates API Key)

```

1.
**Frontend Request**: User clicks "Subscribe" on plan ID `2`. Axios request interceptor attaches header `Authorization: Bearer <JWT>`. Sent to `POST /api/subscriptions/subscribe/2`.


2. **API Gateway Processing**:
* Route `core-subscriptions-route` intercepts path `/api/subscriptions/**`.


*
`JwtAuthenticationFilter` decodes Bearer token using secret key, extracts `email` and `role`.


* Mutates request to append headers: `X-User-Email: user@example.com` and `X-User-Role: ROLE_USER`.


* Routes request downstream to `billify-core-service`.




3. **billify-core-service Processing**:
*
`JwtAuthFilter` detects `X-User-Email` and `X-User-Role`, creating a Spring `UsernamePasswordAuthenticationToken`.


*
`SubscriptionController.subscribe()` delegates to `SubscriptionService.subscribe()`.


* **Database Transaction (`@Transactional`)**:
1. Verifies user has no active subscription.


2. Fetches `Plan` ID `2`.


3. Saves new `Subscription` record with `ACTIVE` status.


4. Invokes `PaymentService.processPayment()` to record transaction.


5. Calls `ApiKeyService.generateApiKey()` to generate a B2B API Key linked to the user.






4.
**Response**: Returns `SubscriptionResponse` containing subscription timeline and raw API Key (`bk_live_...`) for external service integration.



---

## 🛡️ Perspective 2: Platform Administrator Workflows

### Workflow 3: Admin Manages Subscription Plans

**Goal:** Admin creates a new pricing plan (`POST /api/plans`) or deletes an existing one.

```text
[Admin Dashboard] ──> POST /api/plans (Admin Bearer Token)
                          │
                          ▼
                  [API Gateway] (Verifies ROLE_ADMIN in claims)
                          │
                          ▼
              [billify-core-service] (@PreAuthorize("hasRole('ADMIN')"))

```

1.
**Frontend Request**: Admin fills out plan creation form at `/admin/plans` and sends `POST /api/plans`.


2. **API Gateway Processing**:
* Matched by `core-plans-route` (`/api/plans/**`).


*
`JwtAuthenticationFilter` parses token. If role claim is `ROLE_USER` or missing, Gateway blocks request with `401 Unauthorized` or downstream controller throws access denied.


* If role claim is `ROLE_ADMIN`, injects `X-User-Role: ROLE_ADMIN` and forwards to core service.




3. **billify-core-service Processing**:
*
`PlanController.createPlan()` is guarded by `@PreAuthorize("hasRole('ADMIN')")`.


*
`PlanService.createPlan()` executes, persisting plan name, price, limits (`invoiceLimit`, `apiCallLimit`), and duration.




4.
**Response**: Returns `201 Created` with newly generated `PlanResponse`.



---

## ⚡ Perspective 3: B2B External Integration & Multi-Service Orchestration

### Workflow 4: External API Key Request (Invoice Creation Flow)

**Goal:** An external user calls the SaaS Execution service via API Key (`X-API-KEY`) to create an invoice. This triggers inter-service orchestration across **all 3 microservices**.

```text
[External API Client] 
        │ POST /api/invoices (Header: X-API-KEY)
        ▼
[cite_start][API Gateway] (Bypasses JWT filter, injects X-Correlation-ID) [cite: 10, 18, 19]
        │
        ▼
[cite_start][saas-execution-service] ──(Feign 1: Validate Key)──> [billify-core-service] [cite: 2]
        [cite_start]│                ──(Feign 2: Get Quotas)───> [billify-core-service] [cite: 2]
        [cite_start]│                ──(Feign 3: Usage Check)──> [usage-service] [cite: 2]
        │
        [cite_start]├──> [Isolated Local DB Commit in execution_db] [cite: 7, 31, 33, 34]
        │
        [cite_start]└──(Feign 4: Post-Commit Event Push)───────> [usage-service] [cite: 7, 38]

```

#### Detailed Step-by-Step Execution Trace:

1. **Request Ingestion**:
* Client sends `POST /api/invoices` with header `X-API-KEY: bk_live_9a8b...` and body `{ "customerName": "Acme Corp", "amount": 1500.00 }`.


*
**API Gateway**: Matches `execution-invoices-route`.


* Gateway skips `JwtAuthenticationFilter` (since execution uses API Keys) and forwards directly to `saas-execution-service`.


*
`CorrelationIdFilter` assigns `X-Correlation-ID: c56a-4b92-...`.




2.
**Step 1: Remote API Key Validation (Feign Call)**


*
`InvoiceService` in `saas-execution-service` calls `managementClient.validateKey(apiKeyHeader)`.


*
`FeignCorrelationInterceptor` attaches `X-Correlation-ID` to outgoing request.


* HTTP GET sent to `billify-core-service` internal route `/internal/apikeys/validate`.


* Core service checks key hash in DB and returns `userId: 10` and `apiKeyId: 3`.




3.
**Step 2: Active Quota Check (Feign Call)**


*
`InvoiceService` calls `managementClient.getActiveSubscriptionQuota(10)` targeting `billify-core-service` `/internal/subscriptions/quota/10`.


* Core service returns plan quota limits (e.g., `invoiceLimit: 100`).




4.
**Step 3: Usage Query (Feign Call)**


*
`InvoiceService` calls `usageClient.getUsageSummary(10)` targeting `usage-service` `/internal/usage/summary/10`.


* Evaluates whether user's `invoiceCount < invoiceLimit`. If quota is exceeded, throws `IllegalStateException("Invoice quota limit exceeded")`.




5.
**Step 4: Non-Blocking Database Persistence (Isolated Transaction)**


* Request network checks are completed outside active DB transactions.


*
`InvoiceService` delegates to `InvoicePersistenceService.saveInvoice()`.


* Opens `@Transactional` connection, inserts `Invoice` into `execution_db`, commits transaction, and immediately returns connection to pool.




6.
**Step 5: Async Usage Event Propagation (Feign Call)**


* After local database transaction commits, `InvoiceService` executes post-commit network call:


```java
usageClient.createEvent(UsageEventRequest.builder()
    .userId(resolvedUserId)
    .resourceType("INVOICE")
    .apiKeyId(apiKey.getApiKeyId())
    .build());
[cite_start]

```