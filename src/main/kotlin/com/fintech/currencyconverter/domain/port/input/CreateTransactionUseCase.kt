package com.fintech.currencyconverter.domain.port.input

import com.fintech.currencyconverter.domain.model.Transaction
import java.math.BigDecimal
import java.util.UUID

data class CreateTransactionCommand(
    val userId: UUID,
    val idempotencyKey: UUID,
    val sourceCurrency: String,
    val sourceAmount: BigDecimal,
    val targetCurrency: String,
)

interface CreateTransactionUseCase {
    fun execute(command: CreateTransactionCommand): Transaction
}

