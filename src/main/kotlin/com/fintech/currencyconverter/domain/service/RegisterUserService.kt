package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.exception.UserAlreadyExistsException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.input.RegisterUserCommand
import com.fintech.currencyconverter.domain.port.input.RegisterUserUseCase
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RegisterUserService(
    private val userRepository: UserRepositoryPort,
    private val passwordEncoder: PasswordEncoder,
) : RegisterUserUseCase {

    @Transactional
    override fun execute(command: RegisterUserCommand): User {
        if (userRepository.findByEmail(command.email) != null) {
            throw UserAlreadyExistsException(command.email)
        }
        val user = User(
            id = UUID.randomUUID(),
            email = command.email,
            passwordDigest = passwordEncoder.encode(command.rawPassword),
        )
        return userRepository.save(user)
    }
}

