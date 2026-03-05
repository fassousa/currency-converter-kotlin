package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.CreateTransactionCommand
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.util.UUID

@Service
class CreateTransactionService(
    private val transactionRepository: TransactionRepositoryPort,
    private val exchangeRateGateway: ExchangeRateGateway,
) : CreateTransactionUseCase {

    @Transactional
    override fun execute(command: CreateTransactionCommand): Transaction {
        val rate = exchangeRateGateway.getRate(command.sourceCurrency, command.targetCurrency)
        val targetAmount = command.sourceAmount.multiply(rate).setScale(4, RoundingMode.HALF_EVEN)
        val transaction = Transaction(
            id = UUID.randomUUID(),
            userId = command.userId,
            idempotencyKey = command.idempotencyKey,
            sourceCurrency = command.sourceCurrency,
            sourceAmount = command.sourceAmount,
            targetCurrency = command.targetCurrency,
            targetAmount = targetAmount,
            exchangeRate = rate,
        )
        return transactionRepository.save(transaction)
    }
}
