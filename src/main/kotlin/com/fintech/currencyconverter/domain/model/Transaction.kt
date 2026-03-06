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
        /**
         * Factory method that enforces domain invariants on new Transaction creation.
         * Use this when creating a brand-new transaction from business logic.
         * The primary constructor remains accessible for reconstitution from persistence.
         */
        fun create(
            id: UUID,
            userId: UUID,
            idempotencyKey: UUID,
            sourceCurrency: String,
            sourceAmount: BigDecimal,
            targetCurrency: String,
            targetAmount: BigDecimal,
            exchangeRate: BigDecimal,
        ): Transaction {
            require(sourceAmount > BigDecimal.ZERO) { "sourceAmount must be positive" }
            require(targetAmount > BigDecimal.ZERO) { "targetAmount must be positive" }
            require(exchangeRate > BigDecimal.ZERO) { "exchangeRate must be positive" }
            require(sourceCurrency.length == 3 && sourceCurrency == sourceCurrency.uppercase()) {
                "sourceCurrency must be a 3-letter uppercase code"
            }
            require(targetCurrency.length == 3 && targetCurrency == targetCurrency.uppercase()) {
                "targetCurrency must be a 3-letter uppercase code"
            }
            require(sourceCurrency != targetCurrency) { "sourceCurrency and targetCurrency must differ" }
            return Transaction(id, userId, idempotencyKey, sourceCurrency, sourceAmount, targetCurrency, targetAmount, exchangeRate)
        }
    }
}


