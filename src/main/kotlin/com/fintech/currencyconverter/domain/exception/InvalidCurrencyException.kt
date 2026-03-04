package com.fintech.currencyconverter.domain.exception

class InvalidCurrencyException(code: String) : DomainException("Invalid currency code: $code")
