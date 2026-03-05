package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetTransactionsService(
    private val transactionRepository: TransactionRepositoryPort,
) : GetTransactionsUseCase {

    override fun execute(userId: UUID, pageable: Pageable): Page<Transaction> =
        transactionRepository.findByUserId(userId, pageable)
}

