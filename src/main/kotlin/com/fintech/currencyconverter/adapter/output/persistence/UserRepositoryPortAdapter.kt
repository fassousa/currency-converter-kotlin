package com.fintech.currencyconverter.adapter.output.persistence

import com.fintech.currencyconverter.adapter.persistence.mapper.UserMapper
import com.fintech.currencyconverter.adapter.persistence.repository.UserJpaRepository
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserRepositoryPortAdapter(
    private val jpaRepository: UserJpaRepository,
    private val mapper: UserMapper,
) : UserRepositoryPort {

    override fun save(user: User): User =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(user)))

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email)?.let(mapper::toDomain)

    override fun findById(id: UUID): User? =
        jpaRepository.findById(id).orElse(null)?.let(mapper::toDomain)
}

