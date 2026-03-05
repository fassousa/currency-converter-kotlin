package com.fintech.currencyconverter.adapter.input.rest.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val idempotencyKey: UUID,
    val sourceCurrency: String,
    val sourceAmount: BigDecimal,
    val targetCurrency: String,
    val targetAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val createdAt: OffsetDateTime,
)
