
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
