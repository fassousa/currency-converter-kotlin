package com.fintech.currencyconverter.port.inbound

import com.fintech.currencyconverter.domain.model.Currency
import com.fintech.currencyconverter.domain.model.Money
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.UserId
import java.util.UUID

interface ConvertCurrencyUseCase {
    fun convert(
        userId: UserId,
        idempotencyKey: UUID,
        sourceMoney: Money,
        targetCurrency: Currency
    ): Transaction
}
