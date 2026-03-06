package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class UserTest {

    @Test
    fun `create with valid email and password succeeds`() {
        val user = User(id = UUID.randomUUID(), email = "alice@example.com", passwordDigest = "hash")
        assertEquals("alice@example.com", user.email)
        assertEquals("hash", user.passwordDigest)
        assertNotNull(user.id)
    }

    @Test
    fun `two users with same email are equal when ids match`() {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val u1 = User(id = id, email = "alice@example.com", passwordDigest = "hash", createdAt = now, updatedAt = now)
        val u2 = User(id = id, email = "alice@example.com", passwordDigest = "hash", createdAt = now, updatedAt = now)
        assertEquals(u1, u2)
    }
}
