package com.fintech.currencyconverter.port.inbound

import com.fintech.currencyconverter.domain.model.Transaction
import java.math.BigDecimal
import java.util.UUID

interface ConvertCurrencyUseCase {
    fun convert(
        userId: UUID,
        idempotencyKey: UUID,
        sourceCurrency: String,
        sourceAmount: BigDecimal,
        targetCurrency: String,
    ): Transaction
}
