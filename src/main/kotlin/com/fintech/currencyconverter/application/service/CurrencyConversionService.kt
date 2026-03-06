package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.application.event.TransactionCreatedEvent
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import com.fintech.currencyconverter.domain.model.Currency
import com.fintech.currencyconverter.domain.model.Money
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.UserId
import com.fintech.currencyconverter.port.inbound.ConvertCurrencyUseCase
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import com.fintech.currencyconverter.port.outbound.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class CurrencyConversionService(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val exchangeRateGateway: ExchangeRateGateway,
    private val eventPublisher: ApplicationEventPublisher
) : ConvertCurrencyUseCase {

    override fun convert(
        userId: UserId,
        idempotencyKey: UUID,
        sourceMoney: Money,
        targetCurrency: Currency
    ): Transaction {
        // Idempotency check: return existing transaction if key was already processed
        transactionRepository.findByIdempotencyKey(idempotencyKey)?.let { return it }

        // Verify user exists
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        // Fetch live exchange rate from the external gateway
        val rate = exchangeRateGateway.getRate(sourceMoney.currency, targetCurrency)

        // Build and persist the transaction aggregate
        val transaction = Transaction.create(
            userId = userId,
            idempotencyKey = idempotencyKey,
            sourceMoney = sourceMoney,
            targetCurrency = targetCurrency,
            exchangeRate = rate
        )
        val saved = transactionRepository.save(transaction)

        // Publish domain event after successful persistence
        eventPublisher.publishEvent(TransactionCreatedEvent(saved))
        return saved
    }
}

