package com.fintech.currencyconverter.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Transaction(
    val id: TransactionId,
    val userId: UserId,
    val idempotencyKey: UUID,
    val sourceMoney: Money,
    val targetMoney: Money,
    val exchangeRate: BigDecimal,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(sourceMoney.currency != targetMoney.currency) {
            "Source and target currencies must differ"
        }
        require(exchangeRate > BigDecimal.ZERO) {
            "Exchange rate must be positive"
        }
    }

    companion object {
        fun create(
            userId: UserId,
            idempotencyKey: UUID,
            sourceMoney: Money,
            targetCurrency: Currency,
            exchangeRate: BigDecimal
        ): Transaction {
            val targetMoney = sourceMoney.convertTo(targetCurrency, exchangeRate)
            return Transaction(
                id = TransactionId.generate(),
                userId = userId,
                idempotencyKey = idempotencyKey,
                sourceMoney = sourceMoney,
                targetMoney = targetMoney,
                exchangeRate = exchangeRate
            )
        }
    }
}
