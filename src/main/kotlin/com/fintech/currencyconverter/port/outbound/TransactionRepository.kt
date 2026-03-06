package com.fintech.currencyconverter.port.outbound

import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: UUID): Transaction?
    fun findByIdempotencyKey(key: UUID): Transaction?
    fun findAllByUserId(userId: UUID, page: Int, size: Int): List<Transaction>
    fun findByUserId(userId: UUID, pageable: Pageable): Page<Transaction>
}



