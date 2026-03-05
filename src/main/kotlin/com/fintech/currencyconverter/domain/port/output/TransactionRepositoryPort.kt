package com.fintech.currencyconverter.domain.port.output

import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface TransactionRepositoryPort {
    fun save(transaction: Transaction): Transaction
    fun findByUserId(userId: UUID, pageable: Pageable): Page<Transaction>
}
