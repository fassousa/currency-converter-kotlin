package com.fintech.currencyconverter.port.outbound

import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.model.UserId

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UserId): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

