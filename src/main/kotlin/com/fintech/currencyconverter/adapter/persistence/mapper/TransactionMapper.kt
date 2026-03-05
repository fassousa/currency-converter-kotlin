package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.TransactionJpaEntity
import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class TransactionMapper {

    fun toDomain(entity: TransactionJpaEntity): Transaction = Transaction(
        id = entity.id,
        userId = entity.userId,
        idempotencyKey = entity.idempotencyKey,
        sourceCurrency = entity.sourceCurrency,
        sourceAmount = entity.sourceAmount,
        targetCurrency = entity.targetCurrency,
        targetAmount = entity.targetAmount,
        exchangeRate = entity.exchangeRate,
        createdAt = entity.createdAt.atOffset(ZoneOffset.UTC),
    )

    fun toEntity(domain: Transaction): TransactionJpaEntity = TransactionJpaEntity(
        id = domain.id,
        userId = domain.userId,
        idempotencyKey = domain.idempotencyKey,
        sourceCurrency = domain.sourceCurrency,
        sourceAmount = domain.sourceAmount,
        targetCurrency = domain.targetCurrency,
        targetAmount = domain.targetAmount,
        exchangeRate = domain.exchangeRate,
        createdAt = domain.createdAt.toInstant(),
    )
}



