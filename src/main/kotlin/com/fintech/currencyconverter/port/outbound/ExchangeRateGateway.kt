package com.fintech.currencyconverter.port.outbound

import com.fintech.currencyconverter.domain.model.Currency
import java.math.BigDecimal

interface ExchangeRateGateway {
    fun getRate(sourceCurrency: Currency, targetCurrency: Currency): BigDecimal
}

