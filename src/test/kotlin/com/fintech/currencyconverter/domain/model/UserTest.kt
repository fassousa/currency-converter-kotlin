package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserTest {

    @Test
    fun `create with valid email and password succeeds`() {
        val user = User.create("alice@example.com", "hash")
        assertEquals("alice@example.com", user.email)
        assertEquals("hash", user.passwordDigest)
        assertNotNull(user.id)
    }

    @Test
    fun `email without at-sign throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { User.create("invalidemail.com", "hash") }
    }

    @Test
    fun `blank passwordDigest throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { User.create("alice@example.com", "  ") }
    }

    @Test
    fun `blank empty passwordDigest throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { User.create("alice@example.com", "") }
    }
}
