
# Dto's and Mappers:

# ✅ 1. Why DTO at all?

Entity:

```java
@Entity
public class User {

    private Long id;
    private String name;
    private String email;
    private String password;
}
```

We should NOT return this.

Bad:

```java
return user;
```

Client will see password.

So we create DTO.

```java
public class UserDTO {

    private Long id;
    private String name;
    private String email;

}
```

DTO = safe object for API.

---

# ✅ 2. WAY 1 — Manual Mapper (simple way)

This is normal Java.

No library.

Best for Billify.

---

## Step 1 — DTO

```java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String name;
    private String email;

}
```

---

## Step 2 — Mapper class

```java
public class UserMapper {

    public static UserDTO toDTO(User user) {

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

}
```

We manually copy fields.

---

## Step 3 — Use in service

```java
User user = repo.findById(id);

UserDTO dto = UserMapper.toDTO(user);

return dto;
```

Done.

---

## What builder() is doing here

```java
UserDTO.builder()
```

Creates builder object.

Then:

```
.id()
.name()
.email()
.build()
```

build() creates final object.

Equivalent to:

```
new UserDTO(...)
```

Builder just cleaner.

---

## Manual mapper flow

```
Entity → Mapper → DTO → Controller → JSON
```

Simple.

Good for small project.

---

# ✅ 3. WAY 2 — MapStruct Mapper (automatic way)

This uses library.

Used in real projects.

You DON'T write mapping code.

MapStruct generates it.

---

## Step 1 — Add dependency

Maven:

```
mapstruct
mapstruct-processor
```

(we skip for now)

---

## Step 2 — DTO

Same as before.

```java
@Data
public class UserDTO {

    private Long id;
    private String name;
    private String email;

}
```

---

## Step 3 — Mapper interface

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

}
```

You wrote no code.

MapStruct generates it.

---

## Step 4 — Use in service

```java
@Autowired
UserMapper mapper;

UserDTO dto = mapper.toDto(user);
```

Done.

MapStruct generated:

```
UserMapperImpl
```

behind the scenes.

---

# ✅ 4. What about @Mapping ?

Used when fields not same.

Example:

DTO:

```
totalPrice
```

Entity:

```
getTotalPrice()
```

So we write:

```java
@Mapping(
 target = "totalPrice",
 expression = "java(cart.getTotalPrice())"
)
```

Means:

```
dto.totalPrice = cart.getTotalPrice()
```

MapStruct cannot guess this.

So we tell it.

---

# ✅ 5. Important difference

| Feature                | Manual       | MapStruct |
| ---------------------- | ------------ | --------- |
| Easy                   | ✅            | ❌         |
| Fast to write          | ❌            | ✅         |
| Beginner friendly      | ✅            | ❌         |
| Used in big projects   | ⚠️ sometimes | ✅         |
| Good for 4-day project | ✅            | ❌         |
| Needs extra dependency | ❌            | ✅         |

For Billify → manual.

Correct choice.

---

# ✅ 6. Same example both ways

### Manual

```java
public class UserMapper {

    public static UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
```

---

### MapStruct

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

}
```

MapStruct generates code.

---

# ✅ 7. Why your course used MapStruct

Because CodeWithMosh project is bigger.

They want:

* less boilerplate
* cleaner code
* multiple DTOs

But for Billify:

Manual is perfect.

Interviewers like manual also.

---

# ✅ 8. When interviewer asks

Why not return entity?

Answer:

> Entities are persistence objects and may contain sensitive fields or lazy relations. DTOs are used to expose only required data and maintain separation between API and database.

Why mapper?

> Mapper converts between entity and DTO to keep controller clean and maintain layered architecture.

Why builder?

> Builder pattern allows creating objects with many fields in a readable and safe way.

Perfect.

---




# Exception Handling:

## ✅ 1. Why exception handling is needed

Suppose service throws error:

```java
User user = repo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
```

Without handler → Spring returns ugly error:

```json
{
  "timestamp": "...",
  "status": 500,
  "error": "Internal Server Error",
  "trace": "very long stack trace..."
}
```

Bad for API.

We want clean response:

```json
{
  "error": "User not found"
}
```

So we use Global Exception Handler.

---

# ✅ 2. What happens when exception is thrown

Flow:

```
Controller
  ↓
Service
  ↓
Exception thrown
  ↓
Spring catches it
  ↓
Checks @ExceptionHandler
  ↓
Returns ResponseEntity
```

This is automatic.

---

# ✅ 3. What is @RestControllerAdvice

Your code:

```java
@RestControllerAdvice
public class GlobalExceptionHandler
```

This means:

> Apply this class to all controllers

It is combination of:

```
@ControllerAdvice
@ResponseBody
```

So response will be JSON.

---

# ✅ 4. What is @ExceptionHandler

Example:

```java
@ExceptionHandler(ResourceNotFoundException.class)
```

Meaning:

If this exception happens anywhere → call this method.

Spring matches exception type.

---

# ✅ 5. First handler

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<Map<String, String>> handleNotFound(
        ResourceNotFoundException ex)
```

When thrown:

```
throw new ResourceNotFoundException("Plan not found");
```

Spring calls:

```
handleNotFound()
```

Return:

```json
{
  "error": "Plan not found"
}
```

with status 404.

---

### Code

```java
return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", ex.getMessage()));
```

This creates:

```
HTTP 404
{
  "error": "Plan not found"
}
```

---

# ✅ 6. InvalidSubscriptionException

```java
@ExceptionHandler(InvalidSubscriptionException.class)
```

Same idea.

Example:

```
throw new InvalidSubscriptionException(
   "User already has active subscription"
);
```

Response:

```
400 BAD REQUEST
{
  "error": "User already has active subscription"
}
```

Good API design.

---

# ✅ 7. RuntimeException handler

```java
@ExceptionHandler(RuntimeException.class)
```

Catch all runtime errors.

Example:

```
NullPointerException
IllegalArgumentException
etc
```

Instead of crash → return clean error.

Important for production.

---

# ✅ 8. Validation exception (important)

This part:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
```

This happens when using:

```
@Valid
```

Example:

```java
@PostMapping
public void register(@Valid @RequestBody RegisterRequest req)
```

DTO:

```java
@NotNull
@Email
private String email;
```

If invalid → Spring throws:

```
MethodArgumentNotValidException
```

Then this handler runs.

---

## This code

```java
List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .toList();
```

Gets all validation errors.

Example result:

```
email: must be valid
password: size must be >= 6
```

Response:

```json
{
  "errors": [
    "email: must be valid",
    "password: size must be >= 6"
  ]
}
```

This looks professional.

---

# ✅ 9. Your custom exceptions

## ResourceNotFoundException

```java
public class ResourceNotFoundException extends RuntimeException
```

Used when:

```
user not found
plan not found
subscription not found
```

Example:

```java
throw new ResourceNotFoundException("User not found");
```

---

## InvalidSubscriptionException

```java
public class InvalidSubscriptionException extends RuntimeException
```

Used for business rule errors.

Example:

```
already active subscription
invalid upgrade
cancel not allowed
```

Good separation.

Interview question possible.

---

# ✅ 10. Full flow example

Controller:

```java
@GetMapping("/plans/{id}")
public PlanDTO getPlan(@PathVariable Long id) {
    return service.getPlan(id);
}
```

Service:

```java
Plan plan = repo.findById(id)
   .orElseThrow(() ->
       new ResourceNotFoundException("Plan not found")
   );
```

Exception thrown →

Spring sees:

```
@ExceptionHandler(ResourceNotFoundException)
```

Handler runs →

Response:

```
404
{
  "error": "Plan not found"
}
```

Controller never runs again.

This is key idea.

---

# ✅ 11. Why global handler is better than try/catch

Bad:

```java
try {
}
catch(Exception e) {}
```

in every controller.

Messy.

Global handler:

✔ clean
✔ reusable
✔ professional
✔ interview friendly

---

# ✅ 12. Interview answer

What is @RestControllerAdvice?

> It is used to handle exceptions globally across all controllers. Methods annotated with @ExceptionHandler are automatically called when matching exceptions are thrown.

What is MethodArgumentNotValidException?

> It is thrown when validation on a request DTO fails while using @Valid.

What is ResponseEntity?

> Used to control HTTP status and response body.

Good answers.







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

