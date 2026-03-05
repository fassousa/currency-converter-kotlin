package com.fintech.currencyconverter.domain.port.input

import com.fintech.currencyconverter.domain.model.User

data class AuthenticateUserCommand(val email: String, val rawPassword: String)

interface AuthenticateUserUseCase {
    fun execute(command: AuthenticateUserCommand): User
}
