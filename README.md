# URL Shortener

A backend service for a URL Shortener platform, built with a RESTful API architecture. The system generates unique short codes for long URLs and provides fast redirection, along with core features for link management (create, update, expire, custom alias), user management (registration, authentication, and authorization), and click analytics (tracking visits by time, device, and location). The service was designed with scalability and performance in mind, using caching and bloom filtering to optimize redirect speed and support high traffic loads.

---

## 1. Key Features

| Area | Features                                                                                                                                                                 |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Authentication** | Register, login, JWT with access token and refresh token                                                                                                                 |
| **Link management** | Create short links (auto-generated or custom code), update, soft delete, expiration dates                                                                                |
| **Redirect** | `GET /r/{code}` returns 302, served from a Redis cache, with a Bloom filter short-circuiting unknown codes                                                               |
| **Analytics** | Asynchronous click capture through a Redis Stream, and then consumer writes to DB; rollups by hour, lifetime totals, by country; overview / timeseries / by-country APIs |
| **Administration** | Admin manages users (view, enable/disable, delete) and roles                                                                                                             |
| **Infrastructure** | Rate limiting, Caching layer, GeoIP for click location, cleanup job for old clicks                                                                                       |

---

## 2. Technology Stack

### Backend
- **Java 17**, **Spring Boot 4.1.0** (Web MVC, Security, Data JPA, Validation, Cache)
- **PostgreSQL 16** with its schema managed by Flyway
- **Redis Stack 7.4** cache, rate-limit buckets, Redis Stream (analytics), RedisBloom (Bloom filter), JWT blacklist
- **JJWT 0.13.0** JWT generation and validation
- **Bucket4j 8.x** token-bucket rate limiting
- **Springdoc OpenAPI 3.0.3** Swagger UI
- **MaxMind GeoIP2 4.2.1** country/city lookup from IP
- **Lettuce**  Redis client; dispatches native RedisBloom commands

### Infrastructure / CI
- **Docker**
- **docker-compose**
- **GitHub Actions**

---

## 3. Architecture & Components

### 3.1 Backend (`src/main/java/com/example/URLShortener/`)

```
controller/     REST endpoints + RedirectController (/r/{code}) + RedirectFilter
service/        Business interfaces (Auth, ShortUrl, User, Role, Admin, Analytics)
service/impl/   Business implementations
repository/     Spring Data JPA repositories
entity/         JPA entities + enums (User, ShortUrl, Role, RefreshToken, ClickEvent,
                LinkStats, ClickHourly, ClickCountryStats)
dto/request|response/   Inbound/outbound DTOs
logic/          Short-code generation: Base62Encoder, CounterPermutation,
                CounterAllocator, PostgresSequenceCounterAllocator, ShortCodeGenerator
security/       SecurityConfig, CurrentUser, CustomUserDetailService
security/jwt/   JwtTokenProvider, JwtAuthenticationFilter, JwtTokenProperties,
                TokenBlacklistService
cache/          UrlCacheService (redirect cache), BloomFilterService (RedisBloom)
analytic/       ClickEventPublisher/Consumer, ClickEventPayload, GeoIpService,
                ClickEventCleanupJob, AnalyticsStreamConstants
ratelimit/      RateLimitFilter/Service/Aspect, Bucket4jConfig, RateLimitProperties
config/         RedisConfig, CacheConfig, FilterChainConfig, ClickStreamConfig,
                OpenApiConfig, PasswordConfig, BloomFilterWarmupRunner
exception/      Custom exceptions + GlobalExceptionHandler (maps to ErrorResponse)
mapper/         MapStruct mappers (RoleMapper)
```

### 3.3 Backend Request Flow

```
                         ┌───────────────┐
   HTTP ──► RateLimitFilter (-200, /*)  ──► RedirectFilter (-150, /r/*) ──► Security chain (-100)
                         │Bucket4j + Redis       │ reads UrlCacheService
                         └───────────────┘       │
                                                 ├─ cache HIT  ─► publish click (if id present) ─► 302
                                                 └─ cache MISS ─► RedirectController
                                                                     │
                                                     BloomFilterService.mightContain
                                                                     │
                                                     ShortUrlService.getByCodeForRedirect
                                                       ├─ DB fallback
                                                       ├─ write back to cache
                                                       └─ publish click ─► 302
```

**Click capture (asynchronous):**

```
redirect ──XADD──► Redis Stream (urlshortener:stream:click_events)
                          │
                  ClickEventConsumer (1 consumer, poll 1s)
                          │  GeoIP resolve
                          ├─ save ClickEvent (raw)
                          ├─ upsertHourly
                          ├─ upsertLinkStats
                          └─ upsertCountryStats
```

## 4. Installation & Running

### 4.1 Docker

```bash
# Start
docker compose up -d --build
# Stop
docker compose down
```

### 4.2 Run the backend locally

```bash
# Requires Postgres + Redis running (docker compose up -d db redis)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4.3 Maven commands (backend)

```bash
./mvnw clean package -DskipTests      # build the jar
./mvnw test                           # run all tests
./mvnw test -Dtest=ShortUrlServiceImplTest   # run a single test class
```

---

## 5. Testing (backend only)

### 9.1 Unit testing

Run with ./mvnw test. There are 20 test classes with 170 @Test methods in total. This test suite covers all business logic and service classes, ensuring the correctness of core functionalities.

This test suite used JUnit 5 for structuring and executing test cases (including parameterized 
tests, lifecycle annotations, and assertions), and Mockito for mocking dependencies such as reposit
ories, external clients (Redis, geolocation API), and other collaborating services, allowing each 
service class to be tested in isolation without requiring a real database or network connection.

| Group | File | Tests |
|-------|------|------:|
| Service | `AuthServiceImplTest` | 20 |
| | `UserServiceImplTest` | 16 |
| | `ShortUrlServiceImplTest` | 14 |
| | `AnalyticsServiceImplTest` | 14 |
| | `AdminServiceImplTest` | 8 |
| | `RoleServiceImplTest` | 5 |
| Cache | `UrlCacheServiceTest` | 19 |
| | `BloomFilterServiceTest` | 10 |
| Security | `TokenBlacklistServiceTest` | 8 |
| | `JwtAuthenticationFilterTest` | 6 |
| | `JwtTokenProviderTest` | 5 |
| Logic | `Base62EncoderTest` | 5 |
| | `CounterPermutationTest` | 3 |
| | `ShortCodeGeneratorTest` | 2 |
| Analytics | `GeoIpServiceTest` | 8 |
| | `ClickEventConsumerTest` | 6 |
| | `ClickEventPublisherTest` | 3 |
| Rate limit | `RateLimitServiceTest` | 7 |
| Exception | `GlobalExceptionHandlerTest` | 10 |
| Context | `UrlShortenerApplicationTests` | 1 |

### 5.2 Performance testing

Performance testing targets **the redirect endpoint only** (`GET /r/{code}`).

Three metrics are reported: **throughput**, **latency** (avg / p90 / p99), and **stress** (ramp to a breaking point).

**Environment:** single backend instance (Docker, Temurin 17), Postgres 16, Redis Stack 7.4,
k6 v1.4.2. Rate limiting **disabled** (`RATELIMIT_ENABLED=false`). Seed data: 100 hot (pre-warmed in cache) + 10,000 cold (uncached) links.

**T1, Redirect, cache HIT.** `constant-arrival-rate` at 1000 req/s × 5 min, after a 3-minute warmup. Averaged over 3 runs:

| Throughput | avg | p90 | p99 | Error rate |
|-----------:|----:|----:|----:|-----------:|
| **998.4 req/s** | **1.52 ms** | **0.91 ms** | **2.16 ms** | **0%** |

**T2, Redirect, cache MISS.** `shared-iterations`, 50 VUs × 10,000 unique cold codes (one code per request, so every request is a genuine miss). Single run, the codes are cached afterwards:

| Throughput | avg | p90 | p99 | Error rate |
|-----------:|----:|----:|----:|-----------:|
| **3,141.8 req/s** | **15.63 ms** | **26.29 ms** | **41.55 ms** | **0%** |

**T3, Stress.** `ramping-vus` 50 → 100 → 200 → 400 → 800 VUs, 2 minutes per step (11 minutes total), on the cache-HIT path:

| Total requests | Throughput | avg | p90 | p99 | Error rate |
|---------------:|-----------:|----:|----:|----:|-----------:|
| 9,060,166 | **13,727 req/s** | 17.54 ms | 39.33 ms | 54.40 ms | **0%** |
