package com.fintech.currencyconverter.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val amount: BigDecimal, val currency: Currency) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch" }
        return Money(amount + other.amount, currency)
    }

    fun convertTo(targetCurrency: Currency, rate: BigDecimal): Money =
        Money(amount.multiply(rate).setScale(4, RoundingMode.HALF_UP), targetCurrency)
}

