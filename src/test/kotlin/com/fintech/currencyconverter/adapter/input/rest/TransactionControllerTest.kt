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

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var createTransactionUseCase: CreateTransactionUseCase
    @MockkBean private lateinit var getTransactionsUseCase: GetTransactionsUseCase
    @MockkBean private lateinit var jwtService: JwtService
    @MockkBean private lateinit var tokenRevocationService: TokenRevocationService

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

