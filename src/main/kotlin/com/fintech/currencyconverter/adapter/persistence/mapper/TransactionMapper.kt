package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.TransactionJpaEntity
import com.fintech.currencyconverter.domain.model.Currency
import com.fintech.currencyconverter.domain.model.Money
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.TransactionId
import com.fintech.currencyconverter.domain.model.UserId
import org.springframework.stereotype.Component

@Component
class TransactionMapper {

    fun toDomain(entity: TransactionJpaEntity): Transaction = Transaction(
        id = TransactionId(entity.id),
        userId = UserId(entity.userId),
        idempotencyKey = entity.idempotencyKey,
        sourceMoney = Money(entity.sourceAmount, Currency(entity.sourceCurrency)),
        targetMoney = Money(entity.targetAmount, Currency(entity.targetCurrency)),
        exchangeRate = entity.exchangeRate,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: Transaction): TransactionJpaEntity = TransactionJpaEntity(
        id = domain.id.value,
        userId = domain.userId.value,
        idempotencyKey = domain.idempotencyKey,
        sourceCurrency = domain.sourceMoney.currency.code,
        sourceAmount = domain.sourceMoney.amount,
        targetCurrency = domain.targetMoney.currency.code,
        targetAmount = domain.targetMoney.amount,
        exchangeRate = domain.exchangeRate,
        createdAt = domain.createdAt
    )
}

