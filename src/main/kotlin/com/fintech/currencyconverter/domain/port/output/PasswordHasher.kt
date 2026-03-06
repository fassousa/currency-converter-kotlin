package com.fintech.currencyconverter.domain.port.output

interface PasswordHasher {
    fun hash(rawPassword: String): String
    fun matches(rawPassword: String, hashedPassword: String): Boolean
}
