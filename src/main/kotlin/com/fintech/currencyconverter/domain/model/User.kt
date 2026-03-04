package com.fintech.currencyconverter.domain.model

import java.time.Instant

data class User(
    val id: UserId,
    val email: String,
    val passwordDigest: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(email.contains('@')) { "Invalid email: $email" }
        require(passwordDigest.isNotBlank()) { "Password digest must not be blank" }
    }

    companion object {
        fun create(email: String, passwordDigest: String): User =
            User(id = UserId.generate(), email = email, passwordDigest = passwordDigest)
    }
}

