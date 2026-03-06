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

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var registerUserUseCase: RegisterUserUseCase
    @MockkBean private lateinit var authenticateUserUseCase: AuthenticateUserUseCase
    @MockkBean private lateinit var jwtService: JwtService
    @MockkBean private lateinit var tokenRevocationService: TokenRevocationService

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

