package com.fintech.currencyconverter.domain.service

import com.fintech.currencyconverter.domain.model.Money
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.input.CreateTransactionCommand
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import java.math.RoundingMode
import java.util.UUID

class CreateTransactionService(
    private val transactionRepository: TransactionRepositoryPort,
    private val exchangeRateGateway: ExchangeRateGateway,
) : CreateTransactionUseCase {

    override fun execute(command: CreateTransactionCommand): Transaction {
        val rate = exchangeRateGateway.getRate(command.sourceCurrency, command.targetCurrency)
        val targetAmount = command.sourceAmount.multiply(rate).setScale(Money.MONETARY_SCALE, RoundingMode.HALF_EVEN)
        val transaction = Transaction.create(
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

