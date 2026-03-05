package com.fintech.currencyconverter.domain.model

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Transaction(
    val id: UUID,
    val userId: UUID,
    val idempotencyKey: UUID,
    val sourceCurrency: String,
    val sourceAmount: BigDecimal,
    val targetCurrency: String,
    val targetAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
