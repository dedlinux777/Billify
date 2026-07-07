# Billify 💳

Billify is a full-stack subscription management and billing platform designed to allow administrators to configure subscription plans and enable authenticated users to view and manage their subscriptions. The application implements stateless JWT-based authentication, role-based access control, secure password hashing, and global CORS handling.

## 🚀 Tech Stack

### Backend
- **Framework:** Spring Boot 3.x
- **Security:** Spring Security (Stateless JWT Authentication & Authorization)
- **Data Access:** Spring Data JPA
- **Database:** MySQL (Development) / PostgreSQL (Recommended for Free Hosting tiers like Neon/Supabase)
- **Utilities:** Lombok, Jakarta Validation

### Frontend
- **Library:** React (Vite-powered)
- **HTTP Client:** Axios (with automated request/response interceptors)
- **Styling:** Tailwind CSS / Custom CSS

---

## 🔒 Key Security Implementations

1. **Stateless JWT Filter:** Processes inbound HTTP requests, extracts the bearer token from the `Authorization` header, validates signatures/expiration timelines, and injects authenticated user details and mapped roles (`ROLE_USER`, `ROLE_ADMIN`) into the Spring `SecurityContextHolder`.
2. **Global CORS Filter:** Configured explicitly via `CorsFilter` bean to handle cross-origin preflight options and requests originating dynamically from the decoupled frontend client.
3. **Axios Interceptors:** - **Request Interceptor:** Dynamically fetches the signed JWT from browser `localStorage` and appends it as a `Bearer <token>` to the outbound `Authorization` headers.
   - **Response Interceptor:** Automatically clears expired tokens and redirects the client route securely to `/login` whenever the backend issues a `401 Unauthorized` status code.

---

## 📂 Repository Structure

```text
Billify/
├── backend/
│   ├── src/main/java/com/billify/
│   │   ├── auth/           # JwtAuthFilter, JwtService, AuthController, Register/Login DTOs
│   │   ├── config/         # SecurityConfig, CorsConfig, JwtConfig
│   │   ├── dto/            # Data Transfer Objects (PlanDTO, UserDTO, SubscriptionDTO)
│   │   ├── model/          # Entities (User, Plan, Subscription, Payment, Role Enums)
│   │   ├── plan/           # PlanController, PlanService
│   │   ├── repository/     # Spring Data JPA Repositories
│   │   └── subscription/   # Subscription management layers
│   └── src/main/resources/
│       └── application.yaml # Core application configurations
└── frontend/
    ├── src/
    │   ├── components/     # PrivateRoute, AdminRoute
    │   ├── context/        # AuthContext (React Context API for auth state)
    │   ├── pages/          # Login, Register, Plans, AdminPlans, MySubscription
    │   └── services/       # api.js (Axios client config with Interceptors)
    ├── index.html
    └── vite.config.js

```

---

## 🛠️ API Architecture & Endpoint Protection

| Endpoint | HTTP Method | Required Role | Description |
| --- | --- | --- | --- |
| `/api/auth/**` | `ANY` | Permit All | Registration and user login endpoints |
| `/api/plans` | `POST` | `ADMIN` | Create a new subscription plan |
| `/api/plans` | `GET` | `USER` / `ADMIN` | Fetch pageable list of active plans |
| `/api/plans/{id}` | `GET` | `USER` / `ADMIN` | Fetch specific plan details |
| `/api/subscriptions/**` | `ANY` | `USER` / `ADMIN` | Manage user subscriptions |

---

## ⚙️ Local Configuration & Setup

### 1. Backend Setup (Spring Boot)

1. Navigate to the backend directory:
```bash
cd backend

```


2. Configure your properties file in `src/main/resources/application.yaml` or create a `.env` file to provision environment variables:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/billify
    username: YOUR_DB_USERNAME
    password: YOUR_DB_PASSWORD
jwt:
  secret: YOUR_LONG_SUPER_SECRET_HMAC_KEY_BASE64_OR_STRING
  accessTokenExpiration: 3600 # set time configuration carefully
frontend:
  url: http://localhost:5173

```


3. Run the application using the Maven wrapper:
```bash
./mvnw spring-boot:run

```



### 2. Frontend Setup (Vite + React)

1. Navigate to the frontend directory:
```bash
cd frontend

```


2. Install the necessary node modules:
```bash
npm install

```


3. Configure your local environment variables by editing or creating a `.env` file in the frontend root:
```env
VITE_API_BASE_URL=http://localhost:8187

```


4. Start the local development server:
```bash
npm run dev

```



---

## 🌐 Production Deployment Flow (Vercel & Render)

To launch this full stack app online completely on free tiers, implement the following hosting structure:

### Frontend Deployment (Vercel)

1. Push your code to GitHub.
2. Link your repository to **Vercel** and select the `frontend` subfolder as the root directory.
3. Configure the Build Command to `npm run build` and Output Directory to `dist`.
4. Add the Environment Variable: `VITE_API_BASE_URL` pointing to your deployed Render backend URL.

### Backend Deployment (Render)

1. Create a Web Service instance on **Render** linked to your repository.
2. Point the base execution path to the `backend` subdirectory.
3. Set the Environment Variables (`DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_URL`) within Render's Dashboard dashboard settings.

### Database Hosting Recommendation

* Use a serverless/cloud hosting solution like **Neon** or **Supabase** for a free, persistent PostgreSQL cluster.
* Update your `pom.xml` dependency from `mysql-connector-j` to `postgresql` driver and change your configuration profile dialect parameters accordingly to migrate smoothly from MySQL to PostgreSQL.








