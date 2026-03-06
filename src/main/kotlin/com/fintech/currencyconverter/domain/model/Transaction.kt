package com.fintech.currencyconverter.domain.model

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Transaction(
    val id: UUID,
    val userId: UUID,
    val idempotencyKey: UUID,
    val sourceCurrency: String,
    val sourceAmount: BigDecimal,
    val targetCurrency: String,
    val targetAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    companion object {
        fun create(
            id: UUID,
            userId: UUID,
            idempotencyKey: UUID,
            sourceCurrency: String,
            sourceAmount: BigDecimal,
            targetCurrency: String,
            targetAmount: BigDecimal,
            exchangeRate: BigDecimal,
            createdAt: OffsetDateTime = OffsetDateTime.now(),
        ): Transaction {
            require(sourceAmount > BigDecimal.ZERO) { "sourceAmount must be > 0" }
            require(targetAmount > BigDecimal.ZERO) { "targetAmount must be > 0" }
            require(exchangeRate > BigDecimal.ZERO) { "exchangeRate must be > 0" }
            require(sourceCurrency.length == 3 && sourceCurrency == sourceCurrency.uppercase()) {
                "sourceCurrency must be a 3-character uppercase code"
            }
            require(targetCurrency.length == 3 && targetCurrency == targetCurrency.uppercase()) {
                "targetCurrency must be a 3-character uppercase code"
            }
            require(sourceCurrency != targetCurrency) { "sourceCurrency must differ from targetCurrency" }
            return Transaction(id, userId, idempotencyKey, sourceCurrency, sourceAmount, targetCurrency, targetAmount, exchangeRate, createdAt)
        }
    }
}


