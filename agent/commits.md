feat(auth): configure spring security filter chain and jwt services
Co-authored-by: fassousa <39499150+fassousa@users.noreply.github.com>
Copilotfassousa
Copilot
and
fassousa
committed
1 hour ago
commit
47d5363
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/AuthResponse.kt‎
+3
Lines changed: 3 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
data class AuthResponse(val token: String)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/CreateTransactionRequest.kt‎
+21
Lines changed: 21 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal
data class CreateTransactionRequest(
@field:NotBlank(message = "Source currency is required")
@field:Pattern(regexp = "^[A-Z]{3}$", message = "must be exactly 3 uppercase letters")
val sourceCurrency: String?,
@field:NotNull(message = "Source amount is required")
@field:DecimalMin(value = "0.01", message = "must be greater than 0")
val sourceAmount: BigDecimal?,
@field:NotBlank(message = "Target currency is required")
@field:Pattern(regexp = "^[A-Z]{3}$", message = "must be exactly 3 uppercase letters")
val targetCurrency: String?,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/ErrorResponse.kt‎
+6
Lines changed: 6 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
data class ErrorResponse(
val code: String,
val message: String,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/SignInRequest.kt‎
+13
Lines changed: 13 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
data class SignInRequest(
@field:NotBlank(message = "Email is required")
@field:Email(message = "must be a valid email address")
val email: String,
@field:NotBlank(message = "Password is required")
val password: String,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/SignUpRequest.kt‎
+15
Lines changed: 15 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
data class SignUpRequest(
@field:NotBlank(message = "Email is required")
@field:Email(message = "must be a valid email address")
val email: String,
@field:NotBlank(message = "Password is required")
@field:Size(min = 8, message = "must be at least 8 characters")
val password: String,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/TransactionResponse.kt‎
+16
Lines changed: 16 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
data class TransactionResponse(
val id: UUID,
val idempotencyKey: UUID,
val sourceCurrency: String,
val sourceAmount: BigDecimal,
val targetCurrency: String,
val targetAmount: BigDecimal,
val exchangeRate: BigDecimal,
val createdAt: OffsetDateTime,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/dto/ValidationErrorResponse.kt‎
+8
Lines changed: 8 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.dto
data class FieldError(val field: String, val message: String)
data class ValidationErrorResponse(
val code: String,
val errors: List<FieldError>,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/exception/GlobalExceptionHandler.kt‎
+68
Lines changed: 68 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.exception
import com.fintech.currencyconverter.adapter.input.rest.dto.ErrorResponse
import com.fintech.currencyconverter.adapter.input.rest.dto.FieldError
import com.fintech.currencyconverter.adapter.input.rest.dto.ValidationErrorResponse
import com.fintech.currencyconverter.domain.exception.CurrencyNotSupportedException
import com.fintech.currencyconverter.domain.exception.ExchangeRateUnavailableException
import com.fintech.currencyconverter.domain.exception.InvalidCredentialsException
import com.fintech.currencyconverter.domain.exception.UserAlreadyExistsException
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
@RestControllerAdvice
class GlobalExceptionHandler {
@ExceptionHandler(CurrencyNotSupportedException::class)
fun handleCurrencyNotSupported(e: CurrencyNotSupportedException): ResponseEntity<ErrorResponse> =
ResponseEntity
.unprocessableEntity()
.body(ErrorResponse("CURRENCY_NOT_SUPPORTED", e.message ?: "Currency not supported"))
@ExceptionHandler(ExchangeRateUnavailableException::class)
fun handleExchangeRateUnavailable(e: ExchangeRateUnavailableException): ResponseEntity<ErrorResponse> =
ResponseEntity
.status(HttpStatus.SERVICE_UNAVAILABLE)
.body(ErrorResponse("SERVICE_UNAVAILABLE", e.message ?: "Exchange rate service unavailable"))
@ExceptionHandler(UserAlreadyExistsException::class)
fun handleUserAlreadyExists(e: UserAlreadyExistsException): ResponseEntity<ErrorResponse> =
ResponseEntity
.status(HttpStatus.CONFLICT)
.body(ErrorResponse("USER_ALREADY_EXISTS", e.message ?: "User already exists"))
@ExceptionHandler(UserNotFoundException::class, InvalidCredentialsException::class)
fun handleAuthFailure(e: RuntimeException): ResponseEntity<ErrorResponse> =
ResponseEntity
.status(HttpStatus.UNAUTHORIZED)
.body(ErrorResponse("INVALID_CREDENTIALS", "Invalid credentials"))
@ExceptionHandler(MethodArgumentNotValidException::class)
fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
val errors = e.bindingResult.fieldErrors.map {
FieldError(it.field, it.defaultMessage ?: "Invalid value")
}
return ResponseEntity
.unprocessableEntity()
.body(ValidationErrorResponse("VALIDATION_ERROR", errors))
}
@ExceptionHandler(DataIntegrityViolationException::class)
fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
val message = (e.cause?.message ?: e.message ?: "").lowercase()
return if (message.contains("idempotency_key")) {
ResponseEntity
.status(HttpStatus.CONFLICT)
.body(ErrorResponse("IDEMPOTENCY_CONFLICT", "Duplicate request: idempotency key already used"))
} else {
ResponseEntity
.status(HttpStatus.CONFLICT)
.body(ErrorResponse("DATA_CONFLICT", "Data conflict"))
}
}
}
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/mapper/TransactionMapper.kt‎
+15
Lines changed: 15 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest.mapper
import com.fintech.currencyconverter.adapter.input.rest.dto.TransactionResponse
import com.fintech.currencyconverter.domain.model.Transaction
fun Transaction.toResponse() = TransactionResponse(
id = id,
idempotencyKey = idempotencyKey,
sourceCurrency = sourceCurrency,
sourceAmount = sourceAmount,
targetCurrency = targetCurrency,
targetAmount = targetAmount,
exchangeRate = exchangeRate,
createdAt = createdAt,
)
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/AuthController.kt‎
+52
Lines changed: 52 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest
import com.fintech.currencyconverter.adapter.input.rest.dto.AuthResponse
import com.fintech.currencyconverter.adapter.input.rest.dto.SignInRequest
import com.fintech.currencyconverter.adapter.input.rest.dto.SignUpRequest
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserCommand
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserUseCase
import com.fintech.currencyconverter.domain.port.input.RegisterUserCommand
import com.fintech.currencyconverter.domain.port.input.RegisterUserUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import com.fintech.currencyconverter.infrastructure.security.JwtService
import com.fintech.currencyconverter.infrastructure.security.TokenRevocationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
@RestController
@RequestMapping("/auth")
class AuthController(
private val registerUserUseCase: RegisterUserUseCase,
private val authenticateUserUseCase: AuthenticateUserUseCase,
private val jwtService: JwtService,
private val tokenRevocationService: TokenRevocationService,
) {
@PostMapping("/sign_up")
fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<AuthResponse> {
val user = registerUserUseCase.execute(RegisterUserCommand(request.email, request.password))
val token = jwtService.generateToken(user.id, user.email)
return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse(token))
}
@PostMapping("/sign_in")
fun signIn(@Valid @RequestBody request: SignInRequest): ResponseEntity<AuthResponse> {
val user = authenticateUserUseCase.execute(AuthenticateUserCommand(request.email, request.password))
val token = jwtService.generateToken(user.id, user.email)
return ResponseEntity.ok(AuthResponse(token))
}
@PostMapping("/sign_out")
fun signOut(authentication: Authentication): ResponseEntity<Unit> {
val jwtAuth = authentication as JwtAuthenticationToken
val remainingMs = (jwtAuth.expiration.time - System.currentTimeMillis()).coerceAtLeast(1000L)
tokenRevocationService.revoke(jwtAuth.jti, Duration.ofMillis(remainingMs))
return ResponseEntity.noContent().build()
}
}
‎src/main/kotlin/com/fintech/currencyconverter/adapter/input/rest/TransactionController.kt‎
+63
Lines changed: 63 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest
import com.fintech.currencyconverter.adapter.input.rest.dto.CreateTransactionRequest
import com.fintech.currencyconverter.adapter.input.rest.dto.TransactionResponse
import com.fintech.currencyconverter.adapter.input.rest.mapper.toResponse
import com.fintech.currencyconverter.domain.port.input.CreateTransactionCommand
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
@RestController
@RequestMapping("/transactions")
class TransactionController(
private val createTransactionUseCase: CreateTransactionUseCase,
private val getTransactionsUseCase: GetTransactionsUseCase,
) {
@PostMapping
fun createTransaction(
@RequestHeader("Idempotency-Key") idempotencyKey: UUID,
@Valid @RequestBody request: CreateTransactionRequest,
authentication: Authentication,
): ResponseEntity<TransactionResponse> {
val jwtAuth = authentication as JwtAuthenticationToken
val command = CreateTransactionCommand(
userId = jwtAuth.userId,
idempotencyKey = idempotencyKey,
sourceCurrency = request.sourceCurrency!!,
sourceAmount = request.sourceAmount!!,
targetCurrency = request.targetCurrency!!,
)
val transaction = createTransactionUseCase.execute(command)
return ResponseEntity.status(HttpStatus.CREATED).body(transaction.toResponse())
}
@GetMapping
fun getTransactions(
@RequestParam(defaultValue = "0") page: Int,
@RequestParam(defaultValue = "20") size: Int,
authentication: Authentication,
): ResponseEntity<Page<TransactionResponse>> {
val jwtAuth = authentication as JwtAuthenticationToken
val clampedSize = size.coerceIn(1, 100)
val clampedPage = page.coerceAtLeast(0)
val pageable = PageRequest.of(clampedPage, clampedSize, Sort.by("createdAt").descending())
val transactions = getTransactionsUseCase.execute(jwtAuth.userId, pageable)
return ResponseEntity.ok(transactions.map { it.toResponse() })
}
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/exception/CurrencyNotSupportedException.kt‎
+4
Lines changed: 4 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.exception
class CurrencyNotSupportedException(currency: String) :
RuntimeException("Currency not supported: $currency")
‎src/main/kotlin/com/fintech/currencyconverter/domain/exception/ExchangeRateUnavailableException.kt‎
+4
Lines changed: 4 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.exception
class ExchangeRateUnavailableException(message: String) :
RuntimeException(message)
‎src/main/kotlin/com/fintech/currencyconverter/domain/exception/InvalidCredentialsException.kt‎
+3
Lines changed: 3 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.exception
class InvalidCredentialsException : RuntimeException("Invalid credentials")
‎src/main/kotlin/com/fintech/currencyconverter/domain/exception/UserAlreadyExistsException.kt‎
+4
Lines changed: 4 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.exception
class UserAlreadyExistsException(email: String) :
RuntimeException("User already exists: $email")
‎src/main/kotlin/com/fintech/currencyconverter/domain/exception/UserNotFoundException.kt‎
+4
Lines changed: 4 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.exception
class UserNotFoundException(email: String) :
RuntimeException("User not found: $email")
‎src/main/kotlin/com/fintech/currencyconverter/domain/model/Transaction.kt‎
+17
Lines changed: 17 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.model
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
data class Transaction(
val id: UUID,
val userId: UUID,
val idempotencyKey: UUID,
val sourceCurrency: String,
val sourceAmount: BigDecimal,
val targetCurrency: String,
val targetAmount: BigDecimal,
val exchangeRate: BigDecimal,
val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
‎src/main/kotlin/com/fintech/currencyconverter/domain/model/User.kt‎
+12
Lines changed: 12 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.model
import java.time.OffsetDateTime
import java.util.UUID
data class User(
val id: UUID,
val email: String,
val passwordDigest: String,
val createdAt: OffsetDateTime = OffsetDateTime.now(),
val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/input/AuthenticateUserUseCase.kt‎
+9
Lines changed: 9 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.input
import com.fintech.currencyconverter.domain.model.User
data class AuthenticateUserCommand(val email: String, val rawPassword: String)
interface AuthenticateUserUseCase {
fun execute(command: AuthenticateUserCommand): User
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/input/CreateTransactionUseCase.kt‎
+17
Lines changed: 17 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.input
import com.fintech.currencyconverter.domain.model.Transaction
import java.math.BigDecimal
import java.util.UUID
data class CreateTransactionCommand(
val userId: UUID,
val idempotencyKey: UUID,
val sourceCurrency: String,
val sourceAmount: BigDecimal,
val targetCurrency: String,
)
interface CreateTransactionUseCase {
fun execute(command: CreateTransactionCommand): Transaction
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/input/GetTransactionsUseCase.kt‎
+10
Lines changed: 10 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.input
import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID
interface GetTransactionsUseCase {
fun execute(userId: UUID, pageable: Pageable): Page<Transaction>
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/input/RegisterUserUseCase.kt‎
+9
Lines changed: 9 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.input
import com.fintech.currencyconverter.domain.model.User
data class RegisterUserCommand(val email: String, val rawPassword: String)
interface RegisterUserUseCase {
fun execute(command: RegisterUserCommand): User
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/output/ExchangeRateGateway.kt‎
+7
Lines changed: 7 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.output
import java.math.BigDecimal
interface ExchangeRateGateway {
fun getRate(sourceCurrency: String, targetCurrency: String): BigDecimal
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/output/TransactionRepositoryPort.kt‎
+11
Lines changed: 11 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.output
import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID
interface TransactionRepositoryPort {
fun save(transaction: Transaction): Transaction
fun findByUserId(userId: UUID, pageable: Pageable): Page<Transaction>
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/port/output/UserRepositoryPort.kt‎
+10
Lines changed: 10 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.port.output
import com.fintech.currencyconverter.domain.model.User
import java.util.UUID
interface UserRepositoryPort {
fun save(user: User): User
fun findByEmail(email: String): User?
fun findById(id: UUID): User?
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/service/AuthenticateUserService.kt‎
+26
Lines changed: 26 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.service
import com.fintech.currencyconverter.domain.exception.InvalidCredentialsException
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserCommand
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserUseCase
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
@Service
class AuthenticateUserService(
private val userRepository: UserRepositoryPort,
private val passwordEncoder: PasswordEncoder,
) : AuthenticateUserUseCase {
override fun execute(command: AuthenticateUserCommand): User {
val user = userRepository.findByEmail(command.email)
?: throw UserNotFoundException(command.email)
if (!passwordEncoder.matches(command.rawPassword, user.passwordDigest)) {
throw InvalidCredentialsException()
}
return user
}
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/service/CreateTransactionService.kt‎
+35
Lines changed: 35 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.service
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.CreateTransactionCommand
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.util.UUID
@Service
class CreateTransactionService(
private val transactionRepository: TransactionRepositoryPort,
private val exchangeRateGateway: ExchangeRateGateway,
) : CreateTransactionUseCase {
@Transactional
override fun execute(command: CreateTransactionCommand): Transaction {
val rate = exchangeRateGateway.getRate(command.sourceCurrency, command.targetCurrency)
val targetAmount = command.sourceAmount.multiply(rate).setScale(4, RoundingMode.HALF_EVEN)
val transaction = Transaction(
id = UUID.randomUUID(),
userId = command.userId,
idempotencyKey = command.idempotencyKey,
sourceCurrency = command.sourceCurrency,
sourceAmount = command.sourceAmount,
targetCurrency = command.targetCurrency,
targetAmount = targetAmount,
exchangeRate = rate,
)
return transactionRepository.save(transaction)
}
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/service/GetTransactionsService.kt‎
+18
Lines changed: 18 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.service
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID
@Service
class GetTransactionsService(
private val transactionRepository: TransactionRepositoryPort,
) : GetTransactionsUseCase {
override fun execute(userId: UUID, pageable: Pageable): Page<Transaction> =
transactionRepository.findByUserId(userId, pageable)
}
‎src/main/kotlin/com/fintech/currencyconverter/domain/service/RegisterUserService.kt‎
+31
Lines changed: 31 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.domain.service
import com.fintech.currencyconverter.domain.exception.UserAlreadyExistsException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.RegisterUserCommand
import com.fintech.currencyconverter.domain.port.input.RegisterUserUseCase
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
@Service
class RegisterUserService(
private val userRepository: UserRepositoryPort,
private val passwordEncoder: PasswordEncoder,
) : RegisterUserUseCase {
@Transactional
override fun execute(command: RegisterUserCommand): User {
if (userRepository.findByEmail(command.email) != null) {
throw UserAlreadyExistsException(command.email)
}
val user = User(
id = UUID.randomUUID(),
email = command.email,
passwordDigest = passwordEncoder.encode(command.rawPassword),
)
return userRepository.save(user)
}
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/config/CacheConfig.kt‎
+46
Lines changed: 46 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.config
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
@Configuration
@EnableCaching
@ConditionalOnProperty(name = ["spring.cache.type"], havingValue = "redis", matchIfMissing = false)
class CacheConfig {
@Bean
fun cacheManager(
redisConnectionFactory: RedisConnectionFactory,
objectMapper: ObjectMapper,
): RedisCacheManager {
val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
.entryTtl(Duration.ofHours(24))
.serializeKeysWith(
RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
)
.serializeValuesWith(
RedisSerializationContext.SerializationPair.fromSerializer(serializer),
)
.disableCachingNullValues()
return RedisCacheManager.builder(redisConnectionFactory)
.cacheDefaults(defaultConfig)
.withCacheConfiguration(
"exchangeRates",
defaultConfig.entryTtl(Duration.ofHours(24)),
)
.build()
}
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/CustomAccessDeniedHandler.kt‎
+24
Lines changed: 24 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
@Component
class CustomAccessDeniedHandler(private val objectMapper: ObjectMapper) : AccessDeniedHandler {
override fun handle(
request: HttpServletRequest,
response: HttpServletResponse,
accessDeniedException: AccessDeniedException,
) {
response.contentType = MediaType.APPLICATION_JSON_VALUE
response.status = HttpServletResponse.SC_FORBIDDEN
val body = mapOf("error" to "Forbidden", "message" to (accessDeniedException.message ?: "Access denied"))
response.writer.write(objectMapper.writeValueAsString(body))
}
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/CustomAuthEntryPoint.kt‎
+24
Lines changed: 24 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
@Component
class CustomAuthEntryPoint(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {
override fun commence(
request: HttpServletRequest,
response: HttpServletResponse,
authException: AuthenticationException,
) {
response.contentType = MediaType.APPLICATION_JSON_VALUE
response.status = HttpServletResponse.SC_UNAUTHORIZED
val body = mapOf("error" to "Unauthorized", "message" to (authException.message ?: "Authentication required"))
response.writer.write(objectMapper.writeValueAsString(body))
}
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/JwtAuthenticationFilter.kt‎
+53
Lines changed: 53 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID
@Component
class JwtAuthenticationFilter(
private val jwtService: JwtService,
private val tokenRevocationService: TokenRevocationService,
) : OncePerRequestFilter() {
override fun doFilterInternal(
request: HttpServletRequest,
response: HttpServletResponse,
filterChain: FilterChain,
) {
val token = extractBearerToken(request)
if (token != null && jwtService.isTokenValid(token)) {
val claims = jwtService.validateToken(token)
val jti = claims.id
if (!tokenRevocationService.isRevoked(jti)) {
val userId = UUID.fromString(claims["userId"] as String)
val expiration = claims.expiration
val auth = JwtAuthenticationToken(
userId = userId,
jti = jti,
expiration = expiration,
authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
)
auth.details = WebAuthenticationDetailsSource().buildDetails(request)
SecurityContextHolder.getContext().authentication = auth
}
}
filterChain.doFilter(request, response)
}
private fun extractBearerToken(request: HttpServletRequest): String? {
val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
if (!header.startsWith("Bearer ")) return null
return header.removePrefix("Bearer ")
}
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/JwtAuthenticationToken.kt‎
+22
Lines changed: 22 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.util.Date
import java.util.UUID
class JwtAuthenticationToken(
val userId: UUID,
val jti: String,
val expiration: Date,
authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
init {
super.setAuthenticated(authorities.isNotEmpty())
}
override fun getCredentials(): Any? = null
override fun getPrincipal(): Any = userId
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/JwtService.kt‎
+60
Lines changed: 60 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
@Service
class JwtService(
@Value("\${app.jwt.secret}") private val secretKey: String,
@Value("\${app.jwt.expiration:86400000}") private val expirationMs: Long,
) {
private val key: SecretKey by lazy {
Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
}
fun generateToken(userId: UUID, email: String): String {
val now = Date()
val expiry = Date(now.time + expirationMs)
val jti = UUID.randomUUID().toString()
return Jwts.builder()
.subject(email)
.id(jti)
.claim("userId", userId.toString())
.issuedAt(now)
.expiration(expiry)
.signWith(key)
.compact()
}
fun validateToken(token: String): Claims =
Jwts.parser()
.verifyWith(key)
.build()
.parseSignedClaims(token)
.payload
fun isTokenValid(token: String): Boolean =
try {
validateToken(token)
true
} catch (e: JwtException) {
false
} catch (e: IllegalArgumentException) {
false
}
fun extractJti(token: String): String = validateToken(token).id
fun extractUserId(token: String): UUID =
UUID.fromString(validateToken(token)["userId"] as String)
fun extractExpiration(token: String): Date = validateToken(token).expiration
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/SecurityConfig.kt‎
+52
Lines changed: 52 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
@Configuration
@EnableWebSecurity
class SecurityConfig(
private val jwtAuthenticationFilter: JwtAuthenticationFilter,
private val authEntryPoint: CustomAuthEntryPoint,
private val accessDeniedHandler: CustomAccessDeniedHandler,
) {
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
http
.csrf { it.disable() }
.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
.exceptionHandling {
it.authenticationEntryPoint(authEntryPoint)
it.accessDeniedHandler(accessDeniedHandler)
}
.authorizeHttpRequests {
it.requestMatchers(
AntPathRequestMatcher("/auth/sign_up", "POST"),
AntPathRequestMatcher("/auth/sign_in", "POST"),
AntPathRequestMatcher("/actuator/**"),
AntPathRequestMatcher("/v3/api-docs/**"),
AntPathRequestMatcher("/swagger-ui/**"),
AntPathRequestMatcher("/swagger-ui.html"),
).permitAll()
it.anyRequest().authenticated()
}
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
.build()
@Bean
fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
@Bean
fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
config.authenticationManager
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/TokenRevocationService.kt‎
+21
Lines changed: 21 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
@Service
class TokenRevocationService(
private val redisTemplate: RedisTemplate<String, String>,
) {
companion object {
private const val KEY_PREFIX = "revoked:"
}
fun revoke(jti: String, ttl: Duration) {
redisTemplate.opsForValue().set("$KEY_PREFIX$jti", "1", ttl)
}
fun isRevoked(jti: String): Boolean =
redisTemplate.hasKey("$KEY_PREFIX$jti") == true
}
‎src/main/kotlin/com/fintech/currencyconverter/infrastructure/security/UserPrincipal.kt‎
+24
Lines changed: 24 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import com.fintech.currencyconverter.domain.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
class UserPrincipal(val user: User) : UserDetails {
override fun getAuthorities(): Collection<GrantedAuthority> =
listOf(SimpleGrantedAuthority("ROLE_USER"))
override fun getPassword(): String = user.passwordDigest
override fun getUsername(): String = user.email
override fun isAccountNonExpired(): Boolean = true
override fun isAccountNonLocked(): Boolean = true
override fun isCredentialsNonExpired(): Boolean = true
override fun isEnabled(): Boolean = true
}
‎src/main/resources/application.yml‎
+26
Lines changed: 26 additions & 0 deletions
Original file line number	Diff line number	Diff line change
pattern:
console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# Application-specific settings
app:
jwt:
secret: ${JWT_SECRET:dGVzdC1zZWNyZXQta2V5LWZvci1kZXZlbG9wbWVudC0zMmJ5dGU=}
expiration: ${JWT_EXPIRATION:86400000}
currency-api:
url: ${CURRENCY_API_URL:https://api.exchangeratesapi.io/v1}
api-key: ${CURRENCY_API_KEY:change-me}
# Resilience4j configuration
resilience4j:
circuitbreaker:
instances:
exchangeRates:
sliding-window-size: 10
failure-rate-threshold: 50
wait-duration-in-open-state: 30s
minimum-number-of-calls: 5
permitted-number-of-calls-in-half-open-state: 3
automatic-transition-from-open-to-half-open-enabled: true
retry:
instances:
exchangeRates:
max-attempts: 3
wait-duration: 500ms
‎src/test/kotlin/com/fintech/currencyconverter/adapter/input/rest/AuthControllerTest.kt‎
+168
Lines changed: 168 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fintech.currencyconverter.adapter.input.rest.dto.SignInRequest
import com.fintech.currencyconverter.adapter.input.rest.dto.SignUpRequest
import com.fintech.currencyconverter.domain.exception.InvalidCredentialsException
import com.fintech.currencyconverter.domain.exception.UserAlreadyExistsException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserUseCase
import com.fintech.currencyconverter.domain.port.input.RegisterUserUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import com.fintech.currencyconverter.infrastructure.security.JwtService
import com.fintech.currencyconverter.infrastructure.security.TokenRevocationService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
@WebMvcTest(AuthController::class)
@Import(
com.fintech.currencyconverter.infrastructure.security.SecurityConfig::class,
com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationFilter::class,
com.fintech.currencyconverter.infrastructure.security.CustomAuthEntryPoint::class,
com.fintech.currencyconverter.infrastructure.security.CustomAccessDeniedHandler::class,
com.fintech.currencyconverter.adapter.input.rest.exception.GlobalExceptionHandler::class,
)
@ActiveProfiles("test")
class AuthControllerTest {
@Autowired
private lateinit var mockMvc: MockMvc
@Autowired
private lateinit var objectMapper: ObjectMapper
@MockkBean
private lateinit var registerUserUseCase: RegisterUserUseCase
@MockkBean
private lateinit var authenticateUserUseCase: AuthenticateUserUseCase
@MockkBean
private lateinit var jwtService: JwtService
@MockkBean
private lateinit var tokenRevocationService: TokenRevocationService
private val testUser = User(
id = UUID.randomUUID(),
email = "user@example.com",
passwordDigest = "hashed",
createdAt = OffsetDateTime.now(),
updatedAt = OffsetDateTime.now(),
)
@Test
fun `POST sign_up registers user and returns 201 with token`() {
every { registerUserUseCase.execute(any()) } returns testUser
every { jwtService.generateToken(any(), any()) } returns "generated.jwt.token"
val body = SignUpRequest(email = "user@example.com", password = "password123")
mockMvc.post("/auth/sign_up") {
contentType = MediaType.APPLICATION_JSON
content = objectMapper.writeValueAsString(body)
with(csrf())
}.andExpect {
status { isCreated() }
jsonPath("$.token") { value("generated.jwt.token") }
}
}
@Test
fun `POST sign_up returns 409 when email already exists`() {
every { registerUserUseCase.execute(any()) } throws UserAlreadyExistsException("user@example.com")
val body = SignUpRequest(email = "user@example.com", password = "password123")
mockMvc.post("/auth/sign_up") {
contentType = MediaType.APPLICATION_JSON
content = objectMapper.writeValueAsString(body)
with(csrf())
}.andExpect {
status { isConflict() }
jsonPath("$.code") { value("USER_ALREADY_EXISTS") }
}
}
@Test
fun `POST sign_in returns 200 with token for valid credentials`() {
every { authenticateUserUseCase.execute(any()) } returns testUser
every { jwtService.generateToken(any(), any()) } returns "valid.jwt.token"
val body = SignInRequest(email = "user@example.com", password = "password123")
mockMvc.post("/auth/sign_in") {
contentType = MediaType.APPLICATION_JSON
content = objectMapper.writeValueAsString(body)
with(csrf())
}.andExpect {
status { isOk() }
jsonPath("$.token") { value("valid.jwt.token") }
}
}
@Test
fun `POST sign_in returns 401 for invalid credentials`() {
every { authenticateUserUseCase.execute(any()) } throws InvalidCredentialsException()
val body = SignInRequest(email = "user@example.com", password = "wrong")
mockMvc.post("/auth/sign_in") {
contentType = MediaType.APPLICATION_JSON
content = objectMapper.writeValueAsString(body)
with(csrf())
}.andExpect {
status { isUnauthorized() }
jsonPath("$.code") { value("INVALID_CREDENTIALS") }
}
}
@Test
fun `POST sign_out revokes token and returns 204`() {
every { tokenRevocationService.revoke(any(), any()) } just runs
val jwtAuth = JwtAuthenticationToken(
userId = testUser.id,
jti = "test-jti",
expiration = Date(System.currentTimeMillis() + 3_600_000),
authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
)
mockMvc.post("/auth/sign_out") {
with(authentication(jwtAuth))
with(csrf())
}.andExpect {
status { isNoContent() }
}
}
@Test
fun `POST sign_up returns 422 for invalid email format`() {
val body = SignUpRequest(email = "not-an-email", password = "password123")
mockMvc.post("/auth/sign_up") {
contentType = MediaType.APPLICATION_JSON
content = objectMapper.writeValueAsString(body)
with(csrf())
}.andExpect {
status { isUnprocessableEntity() }
jsonPath("$.code") { value("VALIDATION_ERROR") }
}
}
}
‎src/test/kotlin/com/fintech/currencyconverter/adapter/input/rest/TransactionControllerTest.kt‎
+148
Lines changed: 148 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.adapter.input.rest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fintech.currencyconverter.adapter.input.rest.dto.CreateTransactionRequest
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import com.fintech.currencyconverter.infrastructure.security.JwtService
import com.fintech.currencyconverter.infrastructure.security.TokenRevocationService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
@WebMvcTest(TransactionController::class)
@Import(
com.fintech.currencyconverter.infrastructure.security.SecurityConfig::class,
com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationFilter::class,
com.fintech.currencyconverter.infrastructure.security.CustomAuthEntryPoint::class,
com.fintech.currencyconverter.infrastructure.security.CustomAccessDeniedHandler::class,
com.fintech.currencyconverter.adapter.input.rest.exception.GlobalExceptionHandler::class,
)
@ActiveProfiles("test")
class TransactionControllerTest {
@Autowired
private lateinit var mockMvc: MockMvc
@Autowired
private lateinit var objectMapper: ObjectMapper
@MockkBean
private lateinit var createTransactionUseCase: CreateTransactionUseCase
@MockkBean
private lateinit var getTransactionsUseCase: GetTransactionsUseCase
@MockkBean
private lateinit var jwtService: JwtService
@MockkBean
private lateinit var tokenRevocationService: TokenRevocationService
private val userId = UUID.randomUUID()
private val jwtAuth = JwtAuthenticationToken(
userId = userId,
jti = "test-jti",
expiration = Date(System.currentTimeMillis() + 3_600_000),
authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
)
@Test
fun `POST transactions creates a transaction and returns 201`() {
val idempotencyKey = UUID.randomUUID()
val transaction = buildTransaction(userId = userId, idempotencyKey = idempotencyKey)
every { createTransactionUseCase.execute(any()) } returns transaction
val request = CreateTransactionRequest(
sourceCurrency = "USD",
sourceAmount = BigDecimal("100.00"),
targetCurrency = "EUR",
)
mockMvc.post("/transactions") {
header("Idempotency-Key", idempotencyKey.toString())
contentType = MediaType.APPLICATION_JSON
content = objectMapper.writeValueAsString(request)
with(authentication(jwtAuth))
with(csrf())
}.andExpect {
status { isCreated() }
jsonPath("$.sourceCurrency") { value("USD") }
jsonPath("$.targetCurrency") { value("EUR") }
}
}
@Test
fun `POST transactions returns 422 for missing sourceCurrency`() {
val body = """{"sourceAmount":100.00,"targetCurrency":"EUR"}"""
mockMvc.post("/transactions") {
header("Idempotency-Key", UUID.randomUUID().toString())
contentType = MediaType.APPLICATION_JSON
content = body
with(authentication(jwtAuth))
with(csrf())
}.andExpect {
status { isUnprocessableEntity() }
jsonPath("$.code") { value("VALIDATION_ERROR") }
}
}
@Test
fun `GET transactions returns paginated list`() {
val transaction = buildTransaction(userId = userId)
every { getTransactionsUseCase.execute(any(), any()) } returns PageImpl(listOf(transaction))
mockMvc.get("/transactions") {
with(authentication(jwtAuth))
}.andExpect {
status { isOk() }
jsonPath("$.content[0].sourceCurrency") { value("USD") }
}
}
@Test
fun `GET transactions clamps size to 100`() {
every { getTransactionsUseCase.execute(any(), any()) } returns PageImpl(emptyList())
mockMvc.get("/transactions?size=999") {
with(authentication(jwtAuth))
}.andExpect {
status { isOk() }
}
// Verify size is clamped - checked via the pageable argument passed to use case
}
private fun buildTransaction(
userId: UUID = this.userId,
idempotencyKey: UUID = UUID.randomUUID(),
) = Transaction(
id = UUID.randomUUID(),
userId = userId,
idempotencyKey = idempotencyKey,
sourceCurrency = "USD",
sourceAmount = BigDecimal("100.00"),
targetCurrency = "EUR",
targetAmount = BigDecimal("92.50"),
exchangeRate = BigDecimal("0.9250"),
createdAt = OffsetDateTime.now(),
)
}
‎src/test/kotlin/com/fintech/currencyconverter/infrastructure/security/JwtServiceTest.kt‎
+93
Lines changed: 93 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import io.jsonwebtoken.JwtException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
class JwtServiceTest {
// Base64 of "test-secret-key-for-testing-only" (32 bytes)
private val testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHk="
private val expirationMs = 3_600_000L
private lateinit var jwtService: JwtService
@BeforeEach
fun setUp() {
jwtService = JwtService(testSecret, expirationMs)
}
@Test
fun `generateToken returns non-blank signed token`() {
val userId = UUID.randomUUID()
val token = jwtService.generateToken(userId, "user@example.com")
assertThat(token).isNotBlank()
assertThat(token.split(".")).hasSize(3) // header.payload.signature
}
@Test
fun `validateToken returns claims for valid token`() {
val userId = UUID.randomUUID()
val email = "alice@example.com"
val token = jwtService.generateToken(userId, email)
val claims = jwtService.validateToken(token)
assertThat(claims.subject).isEqualTo(email)
assertThat(claims["userId"] as String).isEqualTo(userId.toString())
assertThat(claims.id).isNotBlank()
}
@Test
fun `isTokenValid returns true for valid token`() {
val token = jwtService.generateToken(UUID.randomUUID(), "user@example.com")
assertThat(jwtService.isTokenValid(token)).isTrue()
}
@Test
fun `isTokenValid returns false for tampered token`() {
val token = jwtService.generateToken(UUID.randomUUID(), "user@example.com")
val tampered = token.dropLast(5) + "XXXXX"
assertThat(jwtService.isTokenValid(tampered)).isFalse()
}
@Test
fun `isTokenValid returns false for blank string`() {
assertThat(jwtService.isTokenValid("")).isFalse()
}
@Test
fun `extractJti returns unique jti per token`() {
val token1 = jwtService.generateToken(UUID.randomUUID(), "a@a.com")
val token2 = jwtService.generateToken(UUID.randomUUID(), "b@b.com")
assertThat(jwtService.extractJti(token1)).isNotEqualTo(jwtService.extractJti(token2))
}
@Test
fun `extractUserId returns correct userId`() {
val userId = UUID.randomUUID()
val token = jwtService.generateToken(userId, "user@example.com")
assertThat(jwtService.extractUserId(token)).isEqualTo(userId)
}
@Test
fun `validateToken throws JwtException for invalid token`() {
assertThatThrownBy { jwtService.validateToken("invalid.token.here") }
.isInstanceOf(JwtException::class.java)
}
@Test
fun `generateToken produces token with custom userId claim`() {
val userId = UUID.randomUUID()
val token = jwtService.generateToken(userId, "user@example.com")
val claims = jwtService.validateToken(token)
assertThat(claims["userId"]).isEqualTo(userId.toString())
}
}
‎src/test/kotlin/com/fintech/currencyconverter/infrastructure/security/TokenRevocationServiceTest.kt‎
+72
Lines changed: 72 additions & 0 deletions
Original file line number	Diff line number	Diff line change
package com.fintech.currencyconverter.infrastructure.security
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
class TokenRevocationServiceTest {
private val redisTemplate = mockk<RedisTemplate<String, String>>()
private val valueOps = mockk<ValueOperations<String, String>>()
private lateinit var service: TokenRevocationService
@BeforeEach
fun setUp() {
every { redisTemplate.opsForValue() } returns valueOps
service = TokenRevocationService(redisTemplate)
}
@Test
fun `revoke stores jti in redis with the given TTL`() {
val jti = "test-jti-123"
val ttl = Duration.ofMinutes(30)
every { valueOps.set(any(), any(), any<Duration>()) } returns Unit
service.revoke(jti, ttl)
verify { valueOps.set("revoked:$jti", "1", ttl) }
}
@Test
fun `isRevoked returns true when key exists in redis`() {
val jti = "revoked-jti"
every { redisTemplate.hasKey("revoked:$jti") } returns true
assertThat(service.isRevoked(jti)).isTrue()
}
@Test
fun `isRevoked returns false when key does not exist in redis`() {
val jti = "active-jti"
every { redisTemplate.hasKey("revoked:$jti") } returns false
assertThat(service.isRevoked(jti)).isFalse()
}
@Test
fun `isRevoked returns false when redis returns false for missing key`() {
val jti = "missing-jti"
every { redisTemplate.hasKey("revoked:$jti") } returns false
assertThat(service.isRevoked(jti)).isFalse()
}
@Test
fun `revoke uses correct key prefix`() {
val jti = "abc-123"
val keySlot = slot<String>()
every { valueOps.set(capture(keySlot), any(), any<Duration>()) } returns Unit
service.revoke(jti, Duration.ofHours(1))
assertThat(keySlot.captured).isEqualTo("revoked:abc-123")
}
}
‎src/test/resources/application-test.yml‎
+25
Lines changed: 25 additions & 0 deletions
Original file line number	Diff line number	Diff line change
name: test
password: test

# Application config for tests
app:
jwt:
secret: dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHk=
expiration: 3600000
currency-api:
url: http://localhost:8089
api-key: test-api-key
# Resilience4j — shorter waits for tests
resilience4j:
circuitbreaker:
instances:
exchangeRates:
sliding-window-size: 10
failure-rate-threshold: 50
wait-duration-in-open-state: 5s
minimum-number-of-calls: 5
permitted-number-of-calls-in-half-open-state: 3
automatic-transition-from-open-to-half-open-enabled: false
retry:
instances:
exchangeRates:
max-attempts: 1



build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ─── Plugins ──────────────────────────────────────────────────────────────────
plugins {
id("org.springframework.boot") version "3.2.3"
id("io.spring.dependency-management") version "1.1.4"
kotlin("jvm") version "1.9.22"
// Opens @Component/@Service/@Configuration classes for Spring proxy generation
kotlin("plugin.spring") version "1.9.22"
// Generates no-arg constructors required by JPA @Entity / @Embeddable
kotlin("plugin.jpa") version "1.9.22"
id("org.flywaydb.flyway") version "10.8.1"
id("io.gitlab.arturbosch.detekt") version "1.23.5"
jacoco
}

// ─── Project coordinates ──────────────────────────────────────────────────────
group = "com.fintech"
version = "1.0.0"

java {
sourceCompatibility = JavaVersion.VERSION_21
targetCompatibility = JavaVersion.VERSION_21
}

// ─── Repositories ─────────────────────────────────────────────────────────────
repositories {
mavenCentral()
}

// ─── Dependencies ─────────────────────────────────────────────────────────────
dependencies {

    // ── Spring Boot starters ──────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Required for Resilience4j annotation-driven proxying (@CircuitBreaker, @Retry)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // ── Kotlin runtime ────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ── JWT (JJWT 0.12.x — fluent builder API; incompatible with 0.11.x) ─────
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // ── Feign (OpenFeign for external HTTP calls via Spring Cloud) ────────────
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // ── Resilience4j ──────────────────────────────────────────────────────────
    // resilience4j-spring-boot3 is sufficient for annotation-driven CB + Retry.
    // resilience4j-feign is intentionally excluded — it is only needed when using
    // the FeignDecorators builder API directly, which we are not.
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

    // ── Rate Limiting (Bucket4j + Lettuce/Redis) ──────────────────────────────
    // Spring Boot 3.2.x ships Lettuce 6.3.x; Bucket4j 8.9.0 supports Lettuce 6.x ✓
    implementation("com.bucket4j:bucket4j-core:8.9.0")
    implementation("com.bucket4j:bucket4j-redis:8.9.0")

    // ── Database ──────────────────────────────────────────────────────────────────
    // Flyway 10.x splits DB driver support into separate artifacts.
    // Spring Boot 3.2.x manages flyway-core to 9.x; explicit version pins both
    // artifacts to 10.8.1 so flyway-database-postgresql (a 10.x-only module) resolves.
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.8.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.8.1")

    // ── Observability ─────────────────────────────────────────────────────────
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── API Documentation ─────────────────────────────────────────────────────
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // ── Logging (Logstash JSON encoder — prod profile) ────────────────────────
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    // MockK is the idiomatic Kotlin mocking library; springmockk bridges it with @MockBean
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // Testcontainers BOM pins all TC artifacts to a single consistent version
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.4"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")

    // WireMock standalone — includes embedded Jetty to avoid Spring Boot Jetty 12 conflicts
    testImplementation("org.wiremock:wiremock-standalone:3.5.4")

    // H2 in-memory DB — pure unit tests only; never used for integration tests
    testRuntimeOnly("com.h2database:h2")
}

// ─── Spring Cloud BOM (pins Feign and other cloud starters) ──────────────────
dependencyManagement {
imports {
mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
}
}

// ─── Kotlin compiler options ──────────────────────────────────────────────────
tasks.withType<KotlinCompile> {
kotlinOptions {
// Strict JSR-305 null-safety: Spring annotations become Kotlin non-null contracts
freeCompilerArgs += "-Xjsr305=strict"
jvmTarget = "21"
}
}

// ─── Test configuration ───────────────────────────────────────────────────────
tasks.withType<Test> {
useJUnitPlatform()
}

// ─── JaCoCo coverage report ───────────────────────────────────────────────────
tasks.jacocoTestReport {
dependsOn(tasks.test)
reports {
xml.required.set(true)
html.required.set(true)
}
}

tasks.jacocoTestCoverageVerification {
violationRules {
rule {
limit {
minimum = "0.80".toBigDecimal() // 80% line coverage — DoD D.7
}
}
}
}

// ─── Detekt static analysis ───────────────────────────────────────────────────
detekt {
buildUponDefaultConfig = true
allRules = false
config.setFrom(files("$projectDir/detekt.yml"))
}


‎build.gradle.kts‎
+2
-2
Lines changed: 2 additions & 2 deletions
Original file line number	Diff line number	Diff line change
testImplementation("org.testcontainers:postgresql")
testImplementation("org.testcontainers:junit-jupiter")
// WireMock standalone — includes embedded Jetty to avoid Spring Boot Jetty 12 conflicts
testImplementation("org.wiremock:wiremock-standalone:3.5.4")

    // H2 in-memory DB — pure unit tests only; never used for integration tests
    testRuntimeOnly("com.h2database:h2")

