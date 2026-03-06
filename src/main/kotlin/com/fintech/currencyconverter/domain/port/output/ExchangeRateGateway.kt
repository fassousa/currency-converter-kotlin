package com.fintech.currencyconverter.domain.port.output

import java.math.BigDecimal

interface ExchangeRateGateway {
    fun getRate(sourceCurrency: String, targetCurrency: String): BigDecimal
}

