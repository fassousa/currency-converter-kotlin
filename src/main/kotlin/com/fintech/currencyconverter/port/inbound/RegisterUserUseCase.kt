package com.fintech.currencyconverter.port.inbound

import com.fintech.currencyconverter.domain.model.User

interface RegisterUserUseCase {
    fun register(email: String, rawPassword: String): User
}

