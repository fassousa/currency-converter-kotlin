package com.fintech.currencyconverter.adapter.input.rest.mapper

import com.fintech.currencyconverter.adapter.input.rest.dto.TransactionResponse
import com.fintech.currencyconverter.domain.model.Transaction

fun Transaction.toResponse() = TransactionResponse(
    id = id,
    idempotencyKey = idempotencyKey,
    sourceCurrency = sourceCurrency,
    sourceAmount = sourceAmount,
    targetCurrency = targetCurrency,
    targetAmount = targetAmount,
    exchangeRate = exchangeRate,
    createdAt = createdAt,
)
