package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.exception.InvalidCredentialsException
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserCommand
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserUseCase
import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort

class AuthenticateUserService(
    private val userRepository: UserRepositoryPort,
    private val passwordHasher: PasswordHasher,
) : AuthenticateUserUseCase {

    override fun execute(command: AuthenticateUserCommand): User {
        val user = userRepository.findByEmail(command.email)
            ?: throw UserNotFoundException(command.email)
        if (!passwordHasher.matches(command.rawPassword, user.passwordDigest)) {
            throw InvalidCredentialsException()
        }
        return user
    }
}

