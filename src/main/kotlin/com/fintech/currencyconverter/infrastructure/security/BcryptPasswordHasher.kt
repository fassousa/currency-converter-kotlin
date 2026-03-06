package com.fintech.currencyconverter.infrastructure.security

import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordHasher(
    private val passwordEncoder: PasswordEncoder,
) : PasswordHasher {
    override fun hash(rawPassword: String): String = passwordEncoder.encode(rawPassword)
    override fun matches(rawPassword: String, hashedPassword: String): Boolean =
        passwordEncoder.matches(rawPassword, hashedPassword)
}
