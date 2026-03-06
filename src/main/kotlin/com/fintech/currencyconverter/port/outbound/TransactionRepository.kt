package com.fintech.currencyconverter.port.outbound

import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import java.util.UUID

/**
 * Application port for transaction persistence.
 * Uses framework-free PageResult instead of Spring Data's Page<T>.
 */
interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: UUID): Transaction?
    fun findByIdempotencyKey(key: UUID): Transaction?
    fun findAllByUserId(userId: UUID, page: Int, size: Int): List<Transaction>
    fun findPageByUserId(userId: UUID, page: Int, size: Int): PageResult<Transaction>
}



