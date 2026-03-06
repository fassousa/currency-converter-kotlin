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

    @Suppress("SwallowedException")
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

