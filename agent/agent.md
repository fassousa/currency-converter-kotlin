feat: implement Track C — Security, REST Adapters & Resilient External Gateways (C.1–C.17)
#2
Draft
Copilot
wants to merge 3 commits into
main
from
copilot/implement-rest-adapters
+1.373
-2
Lines changed: 1373 additions & 2 deletions
Conversation0 (0)
Commits3 (3)
Checks0 (0)
Files changed45 (45)
Draft
feat: implement Track C — Security, REST Adapters & Resilient External Gateways (C.1–C.17)#2
Copilot
wants to merge 3 commits into
main
from
copilot/implement-rest-adapters
Conversation
Copilot AI
commented
2 hours ago
•
Completes Track C of the Ruby-on-Rails → Hexagonal Kotlin/Spring Boot 3 migration: stateless JWT security, idempotent REST endpoints, and a resilience-decorated external currency-rate gateway.

Domain foundation
User, Transaction domain models; input ports (CreateTransaction, GetTransactions, RegisterUser, AuthenticateUser) and output ports (UserRepositoryPort, TransactionRepositoryPort, ExchangeRateGateway)
JPA persistence adapters backed by H2 (test) / PostgreSQL (prod)
Security (C.1–C.6)
SecurityConfig — stateless, CSRF off, AntPathRequestMatcher permit-all for POST /auth/sign_up|sign_in, actuator, Swagger; everything else authenticated
JwtService — JJWT 0.12.x fluent API; tokens carry userId + jti custom claims
TokenRevocationService — Redis-backed revoke(jti, ttl) / isRevoked(jti)
JwtAuthenticationFilter — extracts ****** validates → checks Redis revocation → populates SecurityContext
CustomAuthEntryPoint (401 JSON) / CustomAccessDeniedHandler (403 JSON)
REST adapters (C.7–C.13)
GlobalExceptionHandler maps domain exceptions to HTTP codes: CurrencyNotSupported→422, ExchangeRateUnavailable→503, UserAlreadyExists→409, DataIntegrityViolation on idempotency_key→409, MethodArgumentNotValid→422
CreateTransactionRequest uses nullable fields + @NotBlank/@Pattern so missing fields trigger 422 via Bean Validation rather than a 400 deserialization error
TransactionController — POST /transactions (requires Idempotency-Key header); GET /transactions (page size clamped to [1, 100])
AuthController — POST /auth/sign_up|sign_in returns JWT; POST /auth/sign_out revokes the JTI in Redis
External gateway (C.14–C.17)
@Cacheable("exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency")
@CircuitBreaker(name = "exchangeRates", fallbackMethod = "fallback")
@Retry(name = "exchangeRates")
override fun getRate(sourceCurrency: String, targetCurrency: String): BigDecimal { … }

private fun fallback(…, e: Throwable): BigDecimal =
throw ExchangeRateUnavailableException("Exchange rate service unavailable: ${e.message}")
CurrencyApiClient — Feign with @RequestHeader("apikey") injection
Resilience4j config: sliding-window 10, failure-threshold 50%, 30 s open wait, 3 retries
CacheConfig — RedisCacheManager with 24 h TTL + Jackson serialisation; guarded by @ConditionalOnProperty(spring.cache.type=redis) so test contexts load cleanly
Test highlights
CurrencyApiGatewayCircuitBreakerTest — WireMock (wiremock-standalone to avoid Jetty 11/12 conflict with Spring Boot 3.2 BOM) + @DynamicPropertySource; asserts circuit is OPEN after 5 consecutive 500s and zero HTTP requests are made on the next call
@WebMvcTest slices explicitly @Import SecurityConfig + filter/handler beans and use @MockkBean for JwtService/TokenRevocationService — the only reliable pattern when the security config is not auto-scanned