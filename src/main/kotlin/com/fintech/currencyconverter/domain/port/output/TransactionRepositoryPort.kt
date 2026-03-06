package com.fintech.currencyconverter.domain.port.output

import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import java.util.UUID

interface TransactionRepositoryPort {
    fun save(transaction: Transaction): Transaction
    fun findByUserId(userId: UUID, page: Int, size: Int): PageResult<Transaction>
    fun findByIdempotencyKey(idempotencyKey: UUID): Transaction?
}

