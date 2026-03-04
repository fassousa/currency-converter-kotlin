package com.fintech.currencyconverter.adapter.persistence

import com.fintech.currencyconverter.adapter.persistence.mapper.UserMapper
import com.fintech.currencyconverter.adapter.persistence.repository.UserJpaRepository
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.model.UserId
import com.fintech.currencyconverter.port.outbound.UserRepository
import org.springframework.stereotype.Component

@Component
class UserPersistenceAdapter(
    private val jpaRepository: UserJpaRepository,
    private val mapper: UserMapper
) : UserRepository {

    override fun save(user: User): User =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(user)))

    override fun findById(id: UserId): User? =
        jpaRepository.findById(id.value).orElse(null)?.let(mapper::toDomain)

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email)?.let(mapper::toDomain)

    override fun existsByEmail(email: String): Boolean =
        jpaRepository.existsByEmail(email)
}

