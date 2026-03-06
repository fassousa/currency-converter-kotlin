package com.fintech.currencyconverter.infrastructure.security

import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Infrastructure adapter that fulfils the domain PasswordHasher port
 * using Spring's BCryptPasswordEncoder.
 */
class BcryptPasswordHasher(
    private val encoder: BCryptPasswordEncoder = BCryptPasswordEncoder(),
) : PasswordHasher {
    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)
    override fun matches(rawPassword: String, hashedPassword: String): Boolean =
        encoder.matches(rawPassword, hashedPassword)
}

