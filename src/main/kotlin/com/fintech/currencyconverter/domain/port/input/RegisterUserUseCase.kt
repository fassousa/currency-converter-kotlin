package com.fintech.currencyconverter.domain.port.input

import com.fintech.currencyconverter.domain.model.User

data class RegisterUserCommand(val email: String, val rawPassword: String)

interface RegisterUserUseCase {
    fun execute(command: RegisterUserCommand): User
}

