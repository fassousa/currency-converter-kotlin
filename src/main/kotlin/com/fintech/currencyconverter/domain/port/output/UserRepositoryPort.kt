package com.fintech.currencyconverter.domain.port.output

import com.fintech.currencyconverter.domain.model.User
import java.util.UUID

interface UserRepositoryPort {
    fun save(user: User): User
    fun findByEmail(email: String): User?
    fun findById(id: UUID): User?
}
