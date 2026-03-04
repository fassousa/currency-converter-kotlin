package com.fintech.currencyconverter.domain.exception

sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

