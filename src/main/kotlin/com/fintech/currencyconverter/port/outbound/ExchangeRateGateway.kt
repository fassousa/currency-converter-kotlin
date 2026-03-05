package com.fintech.currencyconverter.port.outbound

import java.math.BigDecimal

interface ExchangeRateGateway {
    fun getRate(sourceCurrency: String, targetCurrency: String): BigDecimal
}
