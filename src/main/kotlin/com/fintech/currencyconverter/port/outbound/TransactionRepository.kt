package com.fintech.currencyconverter.port.outbound

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.TransactionId
import com.fintech.currencyconverter.domain.model.UserId
import java.util.UUID

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: TransactionId): Transaction?
    fun findByIdempotencyKey(key: UUID): Transaction?
    fun findAllByUserId(userId: UserId, page: Int, size: Int): List<Transaction>
}

