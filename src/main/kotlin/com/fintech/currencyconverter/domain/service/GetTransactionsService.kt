package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import java.util.UUID

class GetTransactionsService(
    private val transactionRepository: TransactionRepositoryPort,
) : GetTransactionsUseCase {

    override fun execute(userId: UUID, page: Int, size: Int): PageResult<Transaction> =
        transactionRepository.findByUserId(userId, page, size)
}

