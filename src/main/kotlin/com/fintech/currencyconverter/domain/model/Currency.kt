package com.fintech.currencyconverter.domain.model

@JvmInline
value class Currency(val code: String) {
    init {
        require(code.matches(Regex("^[A-Z]{3}$"))) { "Invalid currency code: $code" }
    }

    override fun toString(): String = code

    companion object {
        val USD = Currency("USD")
        val EUR = Currency("EUR")
        val GBP = Currency("GBP")
        val BRL = Currency("BRL")
    }
}

