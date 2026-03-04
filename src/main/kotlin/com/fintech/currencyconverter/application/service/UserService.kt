package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.exception.AuthenticationException
import com.fintech.currencyconverter.domain.exception.EmailAlreadyRegisteredException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.port.inbound.AuthenticateUserUseCase
import com.fintech.currencyconverter.port.inbound.RegisterUserUseCase
import com.fintech.currencyconverter.port.outbound.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : RegisterUserUseCase, AuthenticateUserUseCase {

    override fun register(email: String, rawPassword: String): User {
        if (userRepository.existsByEmail(email)) throw EmailAlreadyRegisteredException(email)
        val user = User.create(email, passwordEncoder.encode(rawPassword))
        return userRepository.save(user)
    }

    override fun authenticate(email: String, rawPassword: String): User {
        val user = userRepository.findByEmail(email) ?: throw AuthenticationException()
        if (!passwordEncoder.matches(rawPassword, user.passwordDigest)) throw AuthenticationException()
        return user
    }
}
