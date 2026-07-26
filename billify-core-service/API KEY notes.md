# 1. What is an API Key?
An API Key is a secret credential issued by a service provider to identify and authorize a client application.

Think about a hotel.

When a guest checks in:

```text
Guest → Receives Room Key
```

The room key proves:

```text
This person is authorized
to access this room.
```

Similarly:

```text
Developer → Receives API Key
```

The API key proves:

```text
This application
is authorized
to use the API.
```

---

# 2. Why API Keys Exist

Suppose Billify exposes:

```http
POST /api/invoices
```

without authentication.

Anyone on the internet could do:

```http
POST /api/invoices
```

and generate unlimited invoices.

You would not know:

```text
Who generated it?
Which customer used it?
Which subscription should be charged?
```

So every request needs identity.

API Keys solve that.

---

# 3. API Key User Perspective

Imagine you are Billify's customer.

You purchase:

```text
Professional Plan
```

After payment:

```text
Billify generates:

sk_live_abcd1234xyz...
```

This becomes your API Key.

---

You store it in your application:

```java
String API_KEY =
"sk_live_abcd1234xyz";
```

Whenever your application calls Billify:

```http
POST /api/invoices
```

it sends:

```http
X-API-KEY: sk_live_abcd1234xyz
```

Example:

```http
POST /api/invoices

X-API-KEY: sk_live_abcd1234xyz

{
   "customerName":"ABC",
   "amount":1000
}
```

Billify receives the request.

Billify verifies the key.

If valid:

```text
Request Allowed
```

Otherwise:

```http
401 Unauthorized
```

---

# 4. SaaS Provider Perspective

Now think like Billify.

When a request arrives:

```http
X-API-KEY: sk_live_abcd1234xyz
```

Billify must answer:

```text
Whose key is this?

Which user owns it?

Is it active?

What plan belongs to that user?
```

before processing the request.

This is the primary responsibility of API key infrastructure.

---

# 5. Typical API Key Structure

Most SaaS companies do not generate random strings blindly.

They use structured formats.

Examples:

Stripe

```text
sk_live_xxxxxxxxxxxxx
```

OpenAI

```text
sk-proj-xxxxxxxxxxxxx
```

GitHub

```text
ghp_xxxxxxxxxxxxx
```

AWS

```text
AKIAxxxxxxxxxxxx
```

Notice something:

```text
Prefix + Secret
```

---

A Billify key could look like:

```text
bk_live_8d7a3f92e41c6d...
```

Where:

```text
bk
```

means:

```text
Billify Key
```

and

```text
live
```

means:

```text
Production Key
```

---

# 6. API Key Lifecycle

An API Key goes through a lifecycle.

Creation

```text
Generate Key
Store Key
Assign To User
```

Usage

```text
Customer Sends Key
```

Validation

```text
Verify Key
```

Monitoring

```text
Track Usage
```

Revocation

```text
Disable Key
```

Deletion

```text
Remove Key
```

---

# 7. Database Design

For Billify:

```java
ApiKey
```

could contain:

```java
id

userId

keyPrefix

keyHash

active

createdAt

lastUsedAt
```

Notice:

```java
keyHash
```

not

```java
keyValue
```

This is important.

---

# 8. Why We Hash API Keys

Imagine your database leaks.

Suppose you store:

```java
sk_live_12345
```

directly.

Database breach:

```text
Attacker gets every API key.
```

Game over.

---

Instead:

Store:

```java
SHA256(sk_live_12345)
```

Example:

```text
8fa7d2abf9e3...
```

Now:

```text
Database leaked
```

does NOT reveal the original key.

Same idea as password hashing.

---

# 9. The Lookup Problem

Now we encounter an interesting challenge.

Suppose request arrives:

```http
X-API-KEY: sk_live_abcd123
```

Database stores:

```text
SHA256(sk_live_abcd123)
```

How do we find the correct record?

---

Many cloud providers solve this using:

```text
Prefix + Secret
```

Example:

```text
bk_live_ABC123.DEF456789XYZ
```

Split into:

```text
Public Identifier

ABC123
```

and

```text
Secret

DEF456789XYZ
```

Database:

```java
id

keyIdentifier

hashedSecret
```

---

Request arrives:

```text
ABC123.DEF456789XYZ
```

Billify:

Step 1

Find:

```java
keyIdentifier=ABC123
```

Step 2

Hash:

```text
DEF456789XYZ
```

Step 3

Compare hashes.

This is how Stripe-like systems work.

---

# 10. API Key Flow in Billify

Let's walk through the future request flow.

---

Phase 1 Milestone 3

Generate Key

```http
POST /api/keys
```

Core Service:

```text
Generate Random Secret

↓

Hash Secret

↓

Store ApiKey

↓

Return Plain Secret Once
```

Important:

The plain key is shown only once.

Just like GitHub and Stripe.

---

Database:

```text
ApiKeys

id=1

user_id=42

identifier=AB123

hash=7f8c2a...

active=true
```

---

Client Stores Key

```text
bk_live_AB123.XYZ987...
```

---

Invoice Request

```http
POST /api/invoices

X-API-KEY:
bk_live_AB123.XYZ987...
```

---

Core Service

```text
Extract Identifier

↓

Find ApiKey Record

↓

Hash Secret Part

↓

Compare

↓

Find User

↓

Continue
```

---

Invoice Created

```text
Invoice Saved
```

---

Feign Call

```text
Usage Service
```

receives:

```json
{
  "userId":42,
  "apiKeyId":1,
  "resourceType":"INVOICE"
}
```

---

Usage Event Saved

```text
UsageEvent
```

Database:

```text
id=1

userId=42

apiKeyId=1

resourceType=INVOICE
```

---

# Why Store apiKeyId in UsageEvent?

This becomes powerful later.

You can answer:

```text
Which API key generated most traffic?
```

```text
Which API key generated invoices?
```

```text
Which API key appears compromised?
```

```text
Which API key should be revoked?
```

Without:

```java
apiKeyId
```

those analytics become difficult.

---

# How Billify Evolves Later

Current Milestone 3:

```text
API Key
↓
Invoice
↓
Usage Event
```

Future:

```text
API Key
   |
   +--> Invoice API
   |
   +--> Email API
   |
   +--> Report API
   |
   +--> PDF API
```

Every request can be tracked through:

```java
apiKeyId
```

allowing Billify to become a true multi-product SaaS platform.

This is why API Keys are much more than "just another authentication method." They become the identity of applications, the foundation of usage metering, billing, analytics, abuse detection, rate limiting, auditing, and ultimately revenue generation in SaaS systems.
