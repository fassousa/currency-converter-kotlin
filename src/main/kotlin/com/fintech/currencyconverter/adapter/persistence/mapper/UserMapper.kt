package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.UserJpaEntity
import com.fintech.currencyconverter.domain.model.User
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class UserMapper {

    fun toDomain(entity: UserJpaEntity): User = User(
        id = entity.id,
        email = entity.email,
        passwordDigest = entity.passwordDigest,
        createdAt = entity.createdAt.atOffset(ZoneOffset.UTC),
        updatedAt = entity.updatedAt.atOffset(ZoneOffset.UTC),
    )

    fun toEntity(domain: User): UserJpaEntity = UserJpaEntity(
        id = domain.id,
        email = domain.email,
        passwordDigest = domain.passwordDigest,
        createdAt = domain.createdAt.toInstant(),
        updatedAt = domain.updatedAt.toInstant(),
    )
}



