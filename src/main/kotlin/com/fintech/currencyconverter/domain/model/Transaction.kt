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
        private val CURRENCY_CODE_REGEX = Regex("^[A-Z]{3}$")

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
            require(CURRENCY_CODE_REGEX.matches(sourceCurrency)) {
                "sourceCurrency must be 3 uppercase letters, got: $sourceCurrency"
            }
            require(CURRENCY_CODE_REGEX.matches(targetCurrency)) {
                "targetCurrency must be 3 uppercase letters, got: $targetCurrency"
            }
            require(sourceCurrency != targetCurrency) {
                "sourceCurrency and targetCurrency must differ"
            }
            require(sourceAmount > BigDecimal.ZERO) { "sourceAmount must be positive" }
            require(targetAmount > BigDecimal.ZERO) { "targetAmount must be positive" }
            require(exchangeRate > BigDecimal.ZERO) { "exchangeRate must be positive" }
            return Transaction(
                id = id,
                userId = userId,
                idempotencyKey = idempotencyKey,
                sourceCurrency = sourceCurrency,
                sourceAmount = sourceAmount,
                targetCurrency = targetCurrency,
                targetAmount = targetAmount,
                exchangeRate = exchangeRate,
                createdAt = createdAt,
            )
        }
    }
}

