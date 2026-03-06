package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.exception.AuthenticationException
import com.fintech.currencyconverter.domain.exception.EmailAlreadyRegisteredException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import com.fintech.currencyconverter.port.inbound.AuthenticateUserUseCase
import com.fintech.currencyconverter.port.inbound.RegisterUserUseCase
import com.fintech.currencyconverter.port.outbound.UserRepository
import java.util.UUID

/**
 * Application service for user registration and authentication.
 * Pure class: no Spring annotations. Transactional boundaries enforced at the JPA adapter layer.
 */
class UserService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) : RegisterUserUseCase, AuthenticateUserUseCase {

    override fun register(email: String, rawPassword: String): User {
        if (userRepository.existsByEmail(email)) throw EmailAlreadyRegisteredException(email)
        val user = User(
            id = UUID.randomUUID(),
            email = email,
            passwordDigest = passwordHasher.hash(rawPassword),
        )
        return userRepository.save(user)
    }

    override fun authenticate(email: String, rawPassword: String): User {
        val user = userRepository.findByEmail(email) ?: throw AuthenticationException()
        if (!passwordHasher.matches(rawPassword, user.passwordDigest)) throw AuthenticationException()
        return user
    }
}



