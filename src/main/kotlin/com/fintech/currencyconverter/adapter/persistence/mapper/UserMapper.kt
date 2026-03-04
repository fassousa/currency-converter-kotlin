package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.UserJpaEntity
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.model.UserId
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toDomain(entity: UserJpaEntity): User = User(
        id = UserId(entity.id),
        email = entity.email,
        passwordDigest = entity.passwordDigest,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: User): UserJpaEntity = UserJpaEntity(
        id = domain.id.value,
        email = domain.email,
        passwordDigest = domain.passwordDigest,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
