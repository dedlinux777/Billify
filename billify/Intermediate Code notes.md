## mapper/PlanMapper.java:
```java

javapackage com.billify.mapper;

import com.billify.dto.PlanDTO;
import com.billify.model.Plan;

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
package com.billify.plan;

import com.billify.dto.PlanDTO;
import com.billify.exception.ResourceNotFoundException;
import com.billify.mapper.PlanMapper;
import com.billify.model.Plan;
import com.billify.repository.PlanRepository;
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

    public PlanDTO createPlan(PlanRequest request) {
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
package com.billify.plan;

import com.billify.dto.PlanDTO;
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
    public ResponseEntity<PlanDTO> createPlan(@Valid @RequestBody PlanRequest request) {
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
package com.billify.subscription;

import com.billify.dto.SubscriptionDTO;
import com.billify.exception.InvalidSubscriptionException;
import com.billify.exception.ResourceNotFoundException;
import com.billify.mapper.SubscriptionMapper;
import com.billify.model.*;
import com.billify.repository.*;
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
    private final PlanRepository         planRepository;
    private final UserRepository         userRepository;

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
package com.billify.payment;

import com.billify.dto.PaymentDTO;
import com.billify.exception.PaymentFailedException;
import com.billify.exception.ResourceNotFoundException;
import com.billify.mapper.PaymentMapper;
import com.billify.model.*;
import com.billify.repository.PaymentRepository;
import com.billify.repository.UserRepository;
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
    private final UserRepository    userRepository;

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





.