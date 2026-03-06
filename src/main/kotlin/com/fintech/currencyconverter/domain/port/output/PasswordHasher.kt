package com.fintech.currencyconverter.domain.port.output

/**
 * Pure domain port for password hashing.
 * No framework dependency — adapters wired in infrastructure layer.
 */
interface PasswordHasher {
    fun hash(rawPassword: String): String
    fun matches(rawPassword: String, hashedPassword: String): Boolean
}

