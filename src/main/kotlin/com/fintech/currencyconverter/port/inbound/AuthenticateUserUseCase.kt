package com.fintech.currencyconverter.port.inbound

import com.fintech.currencyconverter.domain.model.User

interface AuthenticateUserUseCase {
    fun authenticate(email: String, rawPassword: String): User
}
