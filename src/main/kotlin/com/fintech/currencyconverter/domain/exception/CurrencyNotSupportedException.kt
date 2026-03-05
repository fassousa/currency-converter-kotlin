package com.fintech.currencyconverter.domain.exception

class CurrencyNotSupportedException(currency: String) :
    RuntimeException("Currency not supported: $currency")
