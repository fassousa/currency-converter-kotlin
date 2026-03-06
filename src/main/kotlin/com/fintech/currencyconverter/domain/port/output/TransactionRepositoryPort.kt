package com.fintech.currencyconverter.domain.port.output

import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import java.util.UUID

/**
 * Domain port for transaction persistence.
 * Uses framework-free PageResult instead of Spring Data's Page<T>.
 */
interface TransactionRepositoryPort {
    fun save(transaction: Transaction): Transaction
    fun findByIdempotencyKey(key: UUID): Transaction?
    fun findByUserId(userId: UUID, page: Int, size: Int): PageResult<Transaction>
}

