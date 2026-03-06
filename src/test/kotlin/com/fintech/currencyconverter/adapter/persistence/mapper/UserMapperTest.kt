package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.UserJpaEntity
import com.fintech.currencyconverter.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class UserMapperTest {

    private val mapper = UserMapper()

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val now = Instant.now()
        val entity = UserJpaEntity(id, "bob@example.com", "hash", now, now)

        val domain = mapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals("bob@example.com", domain.email)
        assertEquals("hash", domain.passwordDigest)
        assertEquals(now.atOffset(ZoneOffset.UTC), domain.createdAt)
        assertEquals(now.atOffset(ZoneOffset.UTC), domain.updatedAt)
    }

    @Test
    fun `toEntity maps all fields correctly`() {
        val user = User(id = UUID.randomUUID(), email = "alice@example.com", passwordDigest = "digest")

        val entity = mapper.toEntity(user)

        assertEquals(user.id, entity.id)
        assertEquals("alice@example.com", entity.email)
        assertEquals("digest", entity.passwordDigest)
    }

    @Test
    fun `round-trip toDomain then toEntity preserves identity`() {
        val id = UUID.randomUUID()
        val now = Instant.now()
        val entity = UserJpaEntity(id, "c@example.com", "pw", now, now)

        val roundTrip = mapper.toEntity(mapper.toDomain(entity))

        assertEquals(entity.id, roundTrip.id)
        assertEquals(entity.email, roundTrip.email)
        assertEquals(entity.passwordDigest, roundTrip.passwordDigest)
    }
}
