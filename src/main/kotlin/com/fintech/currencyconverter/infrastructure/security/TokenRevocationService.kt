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

