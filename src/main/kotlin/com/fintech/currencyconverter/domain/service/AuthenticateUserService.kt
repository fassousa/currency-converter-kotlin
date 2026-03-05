package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.exception.InvalidCredentialsException
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserCommand
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserUseCase
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticateUserService(
    private val userRepository: UserRepositoryPort,
    private val passwordEncoder: PasswordEncoder,
) : AuthenticateUserUseCase {

    override fun execute(command: AuthenticateUserCommand): User {
        val user = userRepository.findByEmail(command.email)
            ?: throw UserNotFoundException(command.email)
        if (!passwordEncoder.matches(command.rawPassword, user.passwordDigest)) {
            throw InvalidCredentialsException()
        }
        return user
    }
}

