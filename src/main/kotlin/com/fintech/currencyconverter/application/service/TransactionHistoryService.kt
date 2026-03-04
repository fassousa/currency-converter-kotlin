package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.UserId
import com.fintech.currencyconverter.port.inbound.GetTransactionHistoryUseCase
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TransactionHistoryService(
    private val transactionRepository: TransactionRepository
) : GetTransactionHistoryUseCase {

    override fun getHistory(userId: UserId, page: Int, size: Int): List<Transaction> =
        transactionRepository.findAllByUserId(userId, page, size)
}

