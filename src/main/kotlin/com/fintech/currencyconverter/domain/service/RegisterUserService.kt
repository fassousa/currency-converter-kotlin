package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.exception.UserAlreadyExistsException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.RegisterUserCommand
import com.fintech.currencyconverter.domain.port.input.RegisterUserUseCase
import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import java.util.UUID

class RegisterUserService(
    private val userRepository: UserRepositoryPort,
    private val passwordHasher: PasswordHasher,
) : RegisterUserUseCase {

    override fun execute(command: RegisterUserCommand): User {
        if (userRepository.findByEmail(command.email) != null) {
            throw UserAlreadyExistsException(command.email)
        }
        val user = User(
            id = UUID.randomUUID(),
            email = command.email,
            passwordDigest = passwordHasher.hash(command.rawPassword),
        )
        return userRepository.save(user)
    }
}

