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
    fun `revoke uses correct key prefix`() {
        val jti = "abc-123"
        val keySlot = slot<String>()
        every { valueOps.set(capture(keySlot), any(), any<Duration>()) } returns Unit

        service.revoke(jti, Duration.ofHours(1))

        assertThat(keySlot.captured).isEqualTo("revoked:abc-123")
    }
}
