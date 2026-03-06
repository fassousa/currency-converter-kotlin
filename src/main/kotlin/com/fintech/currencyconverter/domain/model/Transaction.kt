package com.fintech.currencyconverter.domain.model

import com.fintech.currencyconverter.domain.exception.InvalidAmountException
import com.fintech.currencyconverter.domain.exception.InvalidCurrencyException
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
            if (sourceAmount <= BigDecimal.ZERO) throw InvalidAmountException("sourceAmount must be positive, got: $sourceAmount")
            if (exchangeRate <= BigDecimal.ZERO) throw InvalidAmountException("exchangeRate must be positive, got: $exchangeRate")
            if (!CURRENCY_CODE_REGEX.matches(sourceCurrency)) throw InvalidCurrencyException(sourceCurrency)
            if (!CURRENCY_CODE_REGEX.matches(targetCurrency)) throw InvalidCurrencyException(targetCurrency)
            if (sourceCurrency == targetCurrency) throw InvalidCurrencyException("$sourceCurrency == $targetCurrency: currencies must differ")
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


