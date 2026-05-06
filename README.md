# Colpix Products API

A RESTful API gateway, written in **Java 17 + Spring Boot 3.3**, that exposes
an authenticated, well-documented Products interface on top of the public
[restful-api.dev](https://restful-api.dev) backend.

## Highlights

- **Custom authentication.** Clients exchange `client_id` + `client_secret` for
  a short-lived opaque bearer token. Token TTL is configurable
  (default **5 minutes**, parametrizable via `colpix.auth.token-ttl`).
- **Token validation on every protected endpoint** with consistent JSON error
  responses for missing, invalid, or expired tokens.
- **Per-client collection scoping.** Each `client_id` maps to one logical
  Collection in the backend. Listing, reading, and updating products are
  scoped to the caller's collection so two clients never see each other's
  data.
- **Layered, testable architecture** — Controller → Service → BackendClient,
  with a thread-safe in-memory token store backed by Caffeine and a
  pluggable `ProductCollectionStore`.
- **OpenAPI 3 / Swagger UI** generated automatically.
- **Comprehensive tests** — unit tests for the token and registry services,
  plus a full MockMvc + WireMock integration suite that exercises the
  end-to-end flow without touching the real backend.

## Tech stack

| Concern              | Choice                                           |
|----------------------|--------------------------------------------------|
| Language / runtime   | Java 17 (also runs on 21)                        |
| Framework            | Spring Boot 3.3                                  |
| Web                  | Spring MVC                                       |
| Security             | Spring Security 6 (custom token filter)          |
| HTTP client          | Spring RestClient                                |
| Token cache          | Caffeine (per-token TTL)                         |
| Secret hashing       | BCrypt                                           |
| API docs             | springdoc-openapi (OpenAPI 3 / Swagger UI)       |
| Validation           | Jakarta Bean Validation                          |
| Tests                | JUnit 5, Mockito, Spring MockMvc, WireMock       |
| Build                | Maven                                            |

## Project layout

```
src/main/java/com/colpix/api
├── ColpixApiApplication.java
├── config           // AppProperties, SecurityConfig, OpenApiConfig, RestClientConfig, PasswordConfig
├── controller       // AuthController, ProductController
├── dto              // AuthRequest/Response, ProductRequest/Response, ErrorResponse
├── exception        // ApiException hierarchy + GlobalExceptionHandler
├── model            // ApiClient, IssuedToken, BackendProduct
├── security         // TokenAuthenticationFilter, AuthenticatedClient, entry-point/handler
└── service          // AuthService, TokenService, ClientRegistry,
                     // ProductService, BackendProductsClient, ProductCollectionStore
```

## API

Base URL: `http://localhost:8080`

### Authentication

| Method | Path                  | Description                                |
|--------|-----------------------|--------------------------------------------|
| POST   | `/api/v1/auth/login`  | Exchange credentials for a bearer token    |

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "client_id": "acme", "client_secret": "acme-secret" }
```

```json
{
  "access_token": "Z9eX...",
  "token_type":   "Bearer",
  "expires_in":   300,
  "issued_at":    "2026-05-04T13:00:00Z",
  "expires_at":   "2026-05-04T13:05:00Z"
}
```

### Products (require `Authorization: Bearer <token>`)

| Method | Path                          | Description                                        |
|--------|-------------------------------|----------------------------------------------------|
| GET    | `/api/v1/products`            | List all products in the caller's collection       |
| GET    | `/api/v1/products/{id}`       | Get one product (must be in the caller's collection) |
| POST   | `/api/v1/products`            | Create a product (added to the caller's collection)  |
| PUT    | `/api/v1/products/{id}`       | Update a product (must be in the caller's collection)|

### Error envelope

Every error response follows the same shape:

```json
{
  "timestamp":  "2026-05-04T13:01:00Z",
  "status":     401,
  "error":      "Unauthorized",
  "code":       "invalid_or_expired_token",
  "message":    "Token is invalid or has expired",
  "path":       "/api/v1/products"
}
```

| HTTP | `code`                       | When                                             |
|------|------------------------------|--------------------------------------------------|
| 400  | `validation_failed`          | Request body fails Bean Validation               |
| 400  | `malformed_request`          | Request body missing or non-JSON                 |
| 401  | `missing_token`              | No `Authorization: Bearer …` header              |
| 401  | `invalid_or_expired_token`   | Token unknown or past its TTL                    |
| 401  | `invalid_credentials`        | Bad `client_id`/`client_secret` at login         |
| 404  | `product_not_found`          | Product not in the caller's collection           |
| 502  | `backend_unavailable`        | restful-api.dev returned an error or is down     |

## Configuration

All knobs live under `colpix.*` in `application.yml`:

```yaml
colpix:
  auth:
    token-ttl: PT5M           # ISO-8601 duration; override with COLPIX_AUTH_TOKEN_TTL
  backend:
    base-url: https://api.restful-api.dev
    connect-timeout: PT5S
    read-timeout: PT15S
  clients:
    - client-id: acme
      client-secret-hash: "$2b$10$..."   # BCrypt
      collection-id: col-acme
```

### Demo credentials (for the reviewer)

| `client_id` | `client_secret`  | Collection      |
|-------------|------------------|-----------------|
| `acme`      | `acme-secret`    | `col-acme`      |
| `contoso`   | `contoso-secret` | `col-contoso`   |

These are pre-hashed with BCrypt in `application.yml`. In production these
would be loaded from a secret store and never committed.

### Override the token TTL

```bash
COLPIX_AUTH_TOKEN_TTL=PT10M mvn spring-boot:run        # 10 minutes
COLPIX_AUTH_TOKEN_TTL=PT30S mvn spring-boot:run        # 30 seconds
```

## Running

### Prerequisites
- JDK 17+ (tested on 17 and 21)
- Maven 3.9+
- Outbound network access to `https://api.restful-api.dev`

### Build & test

```bash
mvn clean verify
```

### Start the app

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Swagger UI is available at
`http://localhost:8080/swagger-ui.html`.

### Quick smoke test with curl

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"client_id":"acme","client_secret":"acme-secret"}' | jq -r .access_token)

curl -s -X POST http://localhost:8080/api/v1/products \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"name":"iPhone 15 Pro","data":{"color":"Titanium","price":1199}}' | jq

curl -s http://localhost:8080/api/v1/products \
    -H "Authorization: Bearer $TOKEN" | jq
```

## Postman

A ready-to-run collection is included at
[`postman/Colpix-API.postman_collection.json`](postman/Colpix-API.postman_collection.json).

Import it into Postman, then:

1. Run **Auth → Login**. The test script captures the bearer token into the
   `access_token` collection variable.
2. Run any of the **Products** requests — they pick up the token automatically
   via collection-level Bearer auth.
3. The **Token validation** folder demonstrates the 401 paths.

## Design notes

- **Why opaque tokens (not JWT)?** The challenge requires server-side expiry,
  including the ability to revoke. Opaque tokens with a Caffeine-backed
  store give us O(1) lookup and per-token expiry without any of JWT's
  trade-offs (key rotation, offline replay, larger headers). JWTs would be
  the right choice if we wanted stateless verification across many nodes,
  but for the spec as stated this is simpler and stronger.
- **Why a local collection store?** restful-api.dev has no concept of
  collections — every object lives in one global namespace. To honour the
  requirement that a `client_id` maps to one Collection resource we keep an
  id-to-collection mapping locally and use it to scope reads, lists, and
  updates. The `ProductCollectionStore` interface keeps that decision
  isolated to one class — swapping in a relational table is a one-class
  change.
- **Why `GET /objects?id=...&id=...` for listing?** The unfiltered
  `GET /objects` on restful-api.dev returns only the canonical 1-13
  records, **not** user-created ones. Querying explicitly by id is the
  documented bulk lookup and is also the most semantically correct
  shape — listing should always return exactly the caller's collection.
- **Why BCrypt for the secret?** Even in a demo configuration we should never
  store plaintext secrets. BCrypt + constant-time comparison plus a dummy
  hash on unknown-client paths keeps timing roughly equal between
  authentication failure modes.
- **Why a separate `ApiException` hierarchy?** Controllers stay free of
  HTTP-specific error code branching; the global handler maps domain
  exceptions to the wire envelope in one place. Adding a new error type is
  one new class.
- **Production gaps (intentional, called out)**: the token store and
  collection store are in-memory. They are isolated behind interfaces so a
  Redis / database implementation drops in cleanly. Rate limiting, refresh
  tokens, and per-client metric tags would be the next reasonable additions.

## License

Apache 2.0
