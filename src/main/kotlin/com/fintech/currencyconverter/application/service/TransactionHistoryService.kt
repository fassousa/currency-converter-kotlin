package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.port.inbound.GetTransactionHistoryUseCase
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import java.util.UUID

/**
 * Application service for retrieving transaction history.
 * Pure class: no Spring annotations. Transactional boundaries enforced at the JPA adapter layer.
 */
class TransactionHistoryService(
    private val transactionRepository: TransactionRepository,
) : GetTransactionHistoryUseCase {

    override fun getHistory(userId: UUID, page: Int, size: Int): List<Transaction> =
        transactionRepository.findAllByUserId(userId, page, size)
}
