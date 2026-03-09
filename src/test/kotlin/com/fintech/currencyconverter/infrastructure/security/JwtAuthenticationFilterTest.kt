package com.fintech.currencyconverter.infrastructure.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Date
import java.util.UUID

class JwtAuthenticationFilterTest {

    // Base64("test-secret-key-for-testing-only") — same secret used in JwtServiceTest
    private val testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHk="
    private val jwtService = JwtService(testSecret, expirationMs = 3_600_000L)

    private val tokenRevocationService = mockk<TokenRevocationService>()
    private val filterChain = mockk<FilterChain>(relaxed = true)

    private val filter = JwtAuthenticationFilter(jwtService, tokenRevocationService)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun requestWithBearer(token: String): MockHttpServletRequest =
        MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $token")
        }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `valid token sets JwtAuthenticationToken on SecurityContext`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "user@example.com")
        val jti = jwtService.extractJti(token)

        every { tokenRevocationService.isRevoked(jti) } returns false

        val request = requestWithBearer(token)
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth).isInstanceOf(JwtAuthenticationToken::class.java)
        val jwtAuth = auth as JwtAuthenticationToken
        assertThat(jwtAuth.userId).isEqualTo(userId)
        assertThat(jwtAuth.jti).isEqualTo(jti)
        assertThat(jwtAuth.isAuthenticated).isTrue()
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `valid token passes userId claim correctly`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "alice@example.com")
        val jti = jwtService.extractJti(token)

        every { tokenRevocationService.isRevoked(jti) } returns false

        filter.doFilter(requestWithBearer(token), MockHttpServletResponse(), filterChain)

        val auth = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        assertThat(auth.userId).isEqualTo(userId)
    }

    @Test
    fun `missing Authorization header leaves SecurityContext unauthenticated`() {
        val request = MockHttpServletRequest() // no Authorization header
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `non-Bearer Authorization header leaves SecurityContext unauthenticated`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Basic dXNlcjpwYXNz")
        }

        filter.doFilter(request, MockHttpServletResponse(), filterChain)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `tampered token leaves SecurityContext unauthenticated`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "user@example.com")
        val tampered = token.dropLast(5) + "XXXXX"

        filter.doFilter(requestWithBearer(tampered), MockHttpServletResponse(), filterChain)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `revoked token leaves SecurityContext unauthenticated`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "user@example.com")
        val jti = jwtService.extractJti(token)

        every { tokenRevocationService.isRevoked(jti) } returns true

        filter.doFilter(requestWithBearer(token), MockHttpServletResponse(), filterChain)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    }

    @Test
    fun `filter always calls filterChain regardless of token validity`() {
        // Even with a bad token the request must continue down the chain;
        // security enforcement is the job of AuthorizationFilter, not this filter.
        val request = requestWithBearer("this.is.invalid")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `token expiration is preserved in JwtAuthenticationToken`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId, "user@example.com")
        val jti = jwtService.extractJti(token)
        val expectedExpiry = jwtService.extractExpiration(token)

        every { tokenRevocationService.isRevoked(jti) } returns false

        filter.doFilter(requestWithBearer(token), MockHttpServletResponse(), filterChain)

        val auth = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        assertThat(auth.expiration).isEqualTo(expectedExpiry)
    }

    @Test
    fun `authenticated token has ROLE_USER authority`() {
        val token = jwtService.generateToken(UUID.randomUUID(), "user@example.com")
        val jti = jwtService.extractJti(token)

        every { tokenRevocationService.isRevoked(jti) } returns false

        filter.doFilter(requestWithBearer(token), MockHttpServletResponse(), filterChain)

        val auth = SecurityContextHolder.getContext().authentication
        assertThat(auth.authorities.map { it.authority }).containsExactly("ROLE_USER")
    }
}

