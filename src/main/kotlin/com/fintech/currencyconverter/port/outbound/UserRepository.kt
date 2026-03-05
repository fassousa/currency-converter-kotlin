package com.fintech.currencyconverter.port.outbound

import com.fintech.currencyconverter.domain.model.User
import java.util.UUID

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}



