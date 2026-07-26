## Mapper/PlanMapper.java:

```java

javapackage com.billing.mapper;

import dto.com.saas.billing.PlanDTO;
import model.com.saas.billing.Plan;

public class PlanMapper {

    public static PlanDTO toDTO(Plan plan) {
        return PlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationInDays(plan.getDurationInDays())
                .build();
    }

    public static Plan toEntity(PlanDTO dto) {
        return Plan.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .durationInDays(dto.getDurationInDays())
                .build();
    }
}
```
Why a toEntity here but not in UserMapper? Because admins create plans via API — you need to convert incoming DTO → entity. Users are only created via registration which has its own RegisterRequest. Always ask: does this flow need both directions?

## plan/PlanService.java

```java
package com.billing.plan;

import dto.com.saas.billing.PlanDTO;
import exception.com.saas.billing.ResourceNotFoundException;
import mapper.com.saas.billing.PlanMapper;
import model.com.saas.billing.Plan;
import repository.com.saas.billing.PlanRepository;
import com.saas.billing.plan.PlanRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;

    public PlanDTO createPlan(PlanRequestDTO request) {
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationInDays(request.getDurationInDays())
                .build();

        Plan saved = planRepository.save(plan);
        log.info("New plan created: {} at price {}", saved.getName(), saved.getPrice());
        return PlanMapper.toDTO(saved);
    }

    public Page<PlanDTO> getAllPlans(Pageable pageable) {
        log.info("Fetching plans - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return planRepository.findAll(pageable)
                .map(PlanMapper::toDTO);
    }

    public PlanDTO getPlanById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found with id: " + id));
        return PlanMapper.toDTO(plan);
    }
}
```
"How does pagination work here?"
planRepository.findAll(pageable) returns a Page<Plan>. Spring Data builds the LIMIT and OFFSET SQL automatically from the Pageable object. You call .map(PlanMapper::toDTO) to convert every element without writing a loop. The response includes total pages, total elements, and current page — all for free.

## plan/PlanController.java

```java
package com.billing.plan;

import com.saas.billing.plan.PlanRequestDTO;
import dto.com.saas.billing.PlanDTO;
import com.saas.billing.plan.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // ADMIN only
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody PlanRequestDTO request) {
        return ResponseEntity.status(201).body(planService.createPlan(request));
    }

    // Anyone authenticated
    @GetMapping
    public ResponseEntity<Page<PlanDTO>> getAllPlans(
            @PageableDefault(size = 5, sort = "price") Pageable pageable) {
        return ResponseEntity.ok(planService.getAllPlans(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanDTO> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }
}
```

    

### One thing to add to SecurityConfig.java — enable @PreAuthorize:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // ← add this
@RequiredArgsConstructor
public class SecurityConfig {
```

Why @PreAuthorize here instead of just SecurityConfig?
Both work. SecurityConfig is better for URL-level rules ("all POST to /plans needs ADMIN"). @PreAuthorize is better for method-level rules, especially when the same URL needs different checks based on context. Using both is fine and shows you know the difference.

```java
package com.billing.subscription;

import dto.com.saas.billing.SubscriptionDTO;
import exception.com.saas.billing.InvalidSubscriptionException;
import exception.com.saas.billing.ResourceNotFoundException;
import mapper.com.saas.billing.SubscriptionMapper;
import com.saas.billing.model.Plan;
import com.saas.billing.model.Subscription;
import com.saas.billing.model.SubscriptionStatus;
import com.saas.billing.model.User;
import com.saas.billing.plan.PlanRepository;
import com.saas.billing.subscription.SubscriptionRepository;
import com.saas.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    // ── SUBSCRIBE ──────────────────────────────────────────
    @Transactional
    public SubscriptionDTO subscribe(String email, Long planId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Enforce one active subscription rule
        boolean hasActive = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .isPresent();

        if (hasActive) {
            throw new InvalidSubscriptionException(
                    "You already have an active subscription. Cancel it before subscribing to a new plan.");
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plan not found with id: " + planId));

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(plan.getDurationInDays()))
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("User {} subscribed to plan {}", email, plan.getName());
        return SubscriptionMapper.toDTO(saved);
    }

    // ── CANCEL ─────────────────────────────────────────────
    @Transactional
    public SubscriptionDTO cancel(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new InvalidSubscriptionException("No active subscription found"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        Subscription saved = subscriptionRepository.save(subscription);

        log.info("User {} cancelled subscription to plan {}",
                email, subscription.getPlan().getName());
        return SubscriptionMapper.toDTO(saved);
    }

    // ── UPGRADE ────────────────────────────────────────────
    @Transactional
    public SubscriptionDTO upgrade(String email, Long newPlanId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Subscription current = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new InvalidSubscriptionException(
                                "No active subscription to upgrade"));

        Plan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plan not found with id: " + newPlanId));

        if (current.getPlan().getId().equals(newPlanId)) {
            throw new InvalidSubscriptionException(
                    "You are already on this plan");
        }

        // Cancel current
        current.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(current);

        // Create new
        LocalDateTime now = LocalDateTime.now();
        Subscription upgraded = Subscription.builder()
                .user(user)
                .plan(newPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(newPlan.getDurationInDays()))
                .build();

        Subscription saved = subscriptionRepository.save(upgraded);
        log.info("User {} upgraded from plan {} to plan {}",
                email, current.getPlan().getName(), newPlan.getName());
        return SubscriptionMapper.toDTO(saved);
    }

    // ── VIEW MY SUBSCRIPTION ───────────────────────────────
    public SubscriptionDTO getMySubscription(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new InvalidSubscriptionException("No active subscription found"));

        return SubscriptionMapper.toDTO(subscription);
    }

    // ── VIEW ALL MY SUBSCRIPTIONS (history) ───────────────
    public List<SubscriptionDTO> getMyHistory(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return subscriptionRepository.findAllByUser(user)
                .stream()
                .map(SubscriptionMapper::toDTO)
                .toList();
    }
}
```

```text
✅ 1. Relationship vs Business Rule (VERY IMPORTANT)

These are different things:

Concept	Meaning
JPA relationship	How data is stored in DB
Business rule	What operations are allowed

Your relationship:

User 1 → many Subscription

Means:

User can have many subscriptions in DB

For history

For audit

For billing records

But business rule says:

Only one ACTIVE subscription at a time

Both can exist together.

This is exactly how real SaaS works.

✅ 2. Why User → OneToMany Subscription is correct

Because we want history.

Example:

User subscribes → BASIC
Later upgrades → PRO
Later upgrades → ENTERPRISE

We must store all.

DB should look like:

id	user_id	plan	status
1	1	BASIC	CANCELLED
2	1	PRO	CANCELLED
3	1	ENTERPRISE	ACTIVE

So yes:

User has many subscriptions.

Correct.

✅ 3. Why we still check active subscription

Because of business rule.

System requirement:

User can have many subscriptions in history,
but only one ACTIVE at a time.

That is why this code exists:

boolean hasActive = subscriptionRepository
.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
.isPresent();

This does NOT mean only one subscription exists.

It means:

Only one with status ACTIVE.

Correct logic.

✅ 4. What would happen without this check

Without this:

User could do:

subscribe BASIC
subscribe PRO
subscribe ENTERPRISE

All ACTIVE.

DB:

id	user	plan	status
1	1	BASIC	ACTIVE
2	1	PRO	ACTIVE
3	1	1	ENTERPRISE

This breaks billing logic.

Real systems do not allow this.

So we enforce rule in service layer.

Correct place.
```
"How do you get the current user in the service?"
The controller extracts the email from Spring Security's SecurityContextHolder and passes it to the service. The service never touches the security context directly — that's the controller's job. Clean separation.

"What happens if two requests try to subscribe simultaneously?"
Good question to raise yourself. With a single findByUserAndStatus check, there's a small race window. The real fix is a unique DB constraint on (user_id, status=ACTIVE) — but for a simulation project, the service-layer check is sufficient and honest to say so.


## PaymentService:

```java
package com.billing.payment;

import dto.com.saas.billing.PaymentDTO;
import exception.com.saas.billing.PaymentFailedException;
import exception.com.saas.billing.ResourceNotFoundException;
import mapper.com.saas.billing.PaymentMapper;
import repository.com.saas.billing.PaymentRepository;
import repository.com.saas.billing.UserRepository;
import com.saas.billing.model.Payment;
import com.saas.billing.model.PaymentStatus;
import com.saas.billing.model.Subscription;
import com.saas.billing.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Called INSIDE a @Transactional method in SubscriptionService.
     * If this throws, the entire transaction (including subscription save) rolls back.
     */
    public Payment processPayment(Subscription subscription) {

        log.info("Processing payment for subscription id: {}, plan: {}, amount: {}",
                subscription.getId(),
                subscription.getPlan().getName(),
                subscription.getPlan().getPrice());

        // Simulate payment success/failure (90% success rate)
        boolean paymentSuccess = new Random().nextInt(10) != 0;

        PaymentStatus status = paymentSuccess
                ? PaymentStatus.SUCCESS
                : PaymentStatus.FAILED;

        Payment payment = Payment.builder()
                .subscription(subscription)
                .amount(subscription.getPlan().getPrice())
                .status(status)
                .paymentDate(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        if (status == PaymentStatus.FAILED) {
            log.error("Payment FAILED for user: {}, plan: {}",
                    subscription.getUser().getEmail(),
                    subscription.getPlan().getName());
            // This exception triggers @Transactional rollback
            throw new PaymentFailedException(
                    "Payment processing failed. Please try again.");
        }

        log.info("Payment SUCCESS for user: {}, amount: {}",
                subscription.getUser().getEmail(),
                subscription.getPlan().getPrice());

        return saved;
    }

    public List<PaymentDTO> getMyPayments(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return paymentRepository.findAllByUser(user)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found with id: " + id));
        return PaymentMapper.toDTO(payment);
    }
}
```

# 1️⃣ What is CORS?

CORS = Cross-Origin Resource Sharing

Browser security rule:

> Frontend running on one origin cannot call backend on another origin unless backend allows it.

Origin = protocol + host + port

Example in your project:

```
Frontend → http://localhost:5173
Backend  → http://localhost:8187
```

Different port → different origin → CORS needed.

Without CORS → browser blocks request before it reaches backend.

Important:

✔ Postman works without CORS
✔ Browser needs CORS

---

# 2️⃣ Why you need CORS in Billify

Your setup:

```
React (5173) → Spring Boot (8187)
```

Browser sends:

```
OPTIONS /api/plans
```

This is called:

```
Preflight request
```

Spring must respond with:

```
Access-Control-Allow-Origin
Access-Control-Allow-Methods
Access-Control-Allow-Headers
```

Your CorsConfig does this.

---

# 3️⃣ Your CorsConfig explained line-by-line

### Class

```java
@Configuration
public class CorsConfig {
```

@Configuration → Spring config class
Spring will create beans from here.

---

### Inject frontend url

```java
@Value("${frontend.url}")
private String frontendUrl;
```

Reads from application.yml

```
frontend:
  url: http://localhost:5173
```

Good practice ✅

Not hardcoding.

---

### Bean

```java
@Bean
public CorsFilter corsFilter()
```

Spring will register this filter in filter chain.

CorsFilter runs before controller.

---

### Create config

```java
CorsConfiguration config = new CorsConfiguration();
```

This object defines CORS rules.

---

### Allow origin

```java
config.addAllowedOrigin(frontendUrl);
```

Only allow this frontend.

Good practice.

Alternative:

```
config.addAllowedOrigin("*")
```

But not safe.

---

### Allow methods

```java
config.addAllowedMethod("*");
```

Allow:

```
GET
POST
PUT
DELETE
OPTIONS
PATCH
```

Good for API.

---

### Allow headers

```java
config.addAllowedHeader("*");
```

Allows:

```
Authorization
Content-Type
Accept
```

Needed for JWT.

Without this → token blocked.

---

### Allow credentials

```java
config.setAllowCredentials(true);
```

Needed when:

```
Authorization header
Cookies
JWT
Session
```

If false → browser removes Authorization.

Important for JWT apps.

---

### Register config

```java
UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();
```

This maps CORS rules to URL paths.

---

### Apply to /api/**

```java
source.registerCorsConfiguration("/api/**", config);
```

Only apply CORS to:

```
/api/plans
/api/auth
/api/subscriptions
```

Not to:

```
/swagger
/actuator
/static
```

Good practice.

---

### Return filter

```java
return new CorsFilter(source);
```

Spring adds this to filter chain.

Flow becomes:

```
Request
 ↓
CorsFilter
 ↓
SecurityFilter
 ↓
Controller
```

---

# 4️⃣ How request works internally

Frontend:

```
GET /api/plans
Authorization: Bearer token
```

Browser sends:

```
OPTIONS /api/plans
```

CorsFilter runs:

```
Check origin
Check method
Check header
Allow
```

Then real request:

```
GET /api/plans
```

Then:

```
JwtFilter
Security
Controller
```

---

# 5️⃣ Is this boilerplate?

YES ✅

This is standard CORS config for:

* Spring Boot + React
* Spring Boot + Angular
* Spring Boot + Next.js
* Spring Boot + Vue

You can reuse it in most projects.

Only change:

```
frontend.url
```

---

# 6️⃣ Boilerplate version (recommended template)

You can reuse this in all projects.

```java
@Configuration
public class CorsConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOrigin(frontendUrl);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
```

---

# 7️⃣ Alternative (Spring Security way)

Modern way:

```
http.cors()
```


# 📝 Java Microservices Notes: Method Overloading in DTO Mappers

### 🚀 The Real-World Problem

We modified our `SubscriptionDTO` by adding a new field: `rawApiKey`. This key is generated **only** when a user creates a brand new subscription or upgrades their plan.

However, the existing `SubscriptionMapper.toDTO(subscription)` was being used across multiple other features in the `SubscriptionService` (like viewing subscription history, canceling, or fetching current details) where **no new API key is generated**.

If we simply modified the single existing mapper method to strictly require an API key parameter, we would have broken compilation across all those other features.

---

### 💡 The Solution: Method Overloading

Instead of creating a completely separate method with a confusing name (e.g., `toDTOWithApiKey`), we used **Method Overloading**. This allowed us to keep the clean, descriptive method name `toDTO` while changing its parameter behavior depending on the business context.

#### 1. The Overloaded Mapper Implementation

```java
package com.saas.billing.mapper;

import com.saas.billing.dto.SubscriptionDTO;
import com.saas.billing.model.Subscription;

public class SubscriptionMapper {

    // Version A: Standard Single-Argument Mapper
    // Used when retrieving history or canceling (where rawApiKey is not applicable)
    public static SubscriptionDTO toDTO(Subscription subscription) {
        // DRY Principle: Reuse Version B by passing null for the apiKey parameter
        return toDTO(subscription, null);
    }

    // Version B: Overloaded Two-Argument Mapper
    // Used when a fresh action occurs and a raw API key needs to be exposed once
    public static SubscriptionDTO toDTO(Subscription subscription, String apiKey) {
        return SubscriptionDTO.builder()
                .id(subscription.getId())
                .planName(subscription.getPlan().getName())
                .planPrice(subscription.getPlan().getPrice())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .rawApiKey(apiKey) // <-- Dynamically maps the key string or null
                .build();
    }
}

```

---

### 🔍 How it plays out in the Service Layer

#### Context 1: Creating a Fresh Subscription (Requires API Key)

When subscribing, we generate a fresh API key token pair and pass it explicitly into the two-argument version of the mapper:

```java
ApiKeyCreateResponse apiKeyResponse = apiKeyService.generateApiKey(user.getId());

// Invokes Version B (Two-Argument Mapper)
return SubscriptionMapper.toDTO(saved, apiKeyResponse.getRawApiKey());

```

#### Context 2: Canceling or Fetching History (No API Key)

When canceling or viewing past records, no API key generation happens. We safely call the original single-argument mapper, which defaults the token value to `null` cleanly behind the scenes:

```java
// Invokes Version A (Single-Argument Mapper)
return SubscriptionMapper.toDTO(saved); 

```



---

# 📝 Microservices Notes: OpenFeign vs. Inbound Controller Routing

### 💡 Core Takeaway

Declaring an `@FeignClient` interface inside a microservice **only creates an internal Java client for outbound communication**. It **does not** automatically generate or expose an inbound HTTP endpoint for external clients like Postman or a frontend application on that service's port.

---

### 1. The Anatomy of an OpenFeign Client

```java
@FeignClient(name = "usage-service", url = "http://localhost:8081")
public interface UsageClient {
    @GetMapping("/api/usage/{userId}")
    UsageSummaryResponse getUsageSummary(@PathVariable("userId") Long userId);
}

```

* **What it actually does:** It acts as a **bridge code engine**. Whenever your internal business logic calls `usageClient.getUsageSummary(1L)`, OpenFeign translates that method call into an actual HTTP `GET` network call targeting `http://localhost:8081/api/usage/1` behind the scenes.
* **What it does NOT do:** It does **not** make `billify-core-service` (port `8187`) listen for inbound requests on `/api/usage/{userId}`.

---

### 2. Deconstructing the 404/401 Cascading Error Flow

When you sent a request to `GET http://localhost:8187/api/usage/1`, a chain reaction occurred:

1. **Missing Controller Mapping (404 Error):** The request arrived at port `8187` (`billify-core-service`). Because no `@RestController` was mapped to listen to `/api/usage/` locally on that port, Spring MVC threw a `NoResourceFoundException` (404 Not Found).
2. **Spring Boot Error Dispatching:** When a 404 occurs, Spring Boot automatically forwards the request internally to its built-in global error page route (`/error`).
3. **Security Masking Interception (401 Error):** The internal redirect to `/error` passed back through the security filter chain. Since `/error` was not explicitly listed as a `.permitAll()` endpoint, the security filters evaluated it under `.anyRequest().authenticated()`. Because the original request context was broken during the 404 dispatch, your custom `AuthenticationEntryPoint` caught it and overrode the response to a **401 Unauthorized**.

---

### 3. The Two Correct Ways to Route Requests

#### Strategy A: Direct Microservice Ingestion (Standard Testing)

If you want to read metrics data directly during backend manual testing, you bypass the core service entirely and target the microservice holding the database resource directly:

* **Target Endpoint:** `GET http://localhost:8081/api/usage/1`

#### Strategy B: The Edge Proxy Gateway Pattern

If your frontend or Postman clients are strictly forbidden from hitting individual microservice ports directly, you build an explicit proxy bridge endpoint inside your gateway service (or Core Service):

```java
@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class CoreUsageProxyController {

    private final UsageClient usageClient; // Injected internal OpenFeign proxy client

    @GetMapping("/{userId}")
    public ResponseEntity<UsageSummaryResponse> proxyGetUsageSummary(@PathVariable Long userId) {
        // Explicitly intercept the request on port 8187 and forward it internally via Feign to port 8081
        return ResponseEntity.ok(usageClient.getUsageSummary(userId));
    }
}

```

*(Remembering to add `.requestMatchers("/api/usage/").permitAll()` or `.authenticated()` to your `SecurityConfig` mapping to avoid authorization drops).*

















---

# Adding Eureka to Saas Core:

Where Billify Stands for the moment:
Current architecture:

```text
                 React

                   │

                   ▼

         Billify Core Service
                  │
                  │ OpenFeign
                  ▼
            Usage Service
```

Core Service knows exactly where Usage Service lives.

Inside your configuration you probably have something similar to:

```properties
services.usage-service.url=http://localhost:8081
```

Although we externalized it in Milestone 1, it is still fundamentally a **fixed location**.

---

# The Problem

Imagine six months from now.

Billify is deployed on Kubernetes.

You decide to scale Usage Service.

Instead of:

```text
Usage Service

localhost:8081
```

You now have:

```text
Usage Service Instance 1

10.1.4.18:8081

Usage Service Instance 2

10.1.4.22:8081

Usage Service Instance 3

10.1.4.30:8081
```

Question:

How does Core Service know which one to call?

Hardcoding isn't possible anymore.

If instance 1 crashes:

```text
Core

↓

10.1.4.18

↓

Connection Refused
```

Everything breaks.

This is exactly the problem Eureka was invented to solve.

---

# Eureka's Responsibility

Think of Eureka as a phone directory.

Instead of:

```text
Core

↓

http://localhost:8081
```

Core asks:

```text
Where is

billify-usage-service?
```

Eureka replies:

```text
Available Instances

10.1.4.18

10.1.4.22

10.1.4.30
```

Then the client automatically selects one.

Core no longer cares where Usage Service is running.

It only knows its logical name.

---

# Billify After Milestone 2

Instead of:

```text
             Core
               │
               │ localhost:8081
               ▼
            Usage
```

We'll have:

```text
                 Eureka Server

                       ▲

         Registers     │      Registers

      Core ────────────┼──────────── Usage



Core

↓

Feign

↓

billify-usage-service

↓

Eureka

↓

One Available Instance
```

Notice something important.

Core never sees an IP address anymore.

---

# What We Should Learn

This milestone is **not about Eureka APIs**.

It's about understanding:

* Service Discovery
* Client-side Load Balancing
* Dynamic Service Registration
* Heartbeats
* Registry
* Failure Detection

Those are the real concepts.

---

