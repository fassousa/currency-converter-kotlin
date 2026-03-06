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

