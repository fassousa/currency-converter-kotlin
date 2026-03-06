package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.application.event.TransactionCreatedEvent
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.port.inbound.ConvertCurrencyUseCase
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import org.springframework.context.ApplicationEventPublisher
import java.math.RoundingMode
import java.util.UUID

/**
 * Application service for currency conversion.
 * Pure class: no Spring annotations. Transactional boundaries enforced at the JPA adapter layer.
 */
class CurrencyConversionService(
    private val transactionRepository: TransactionRepository,
    private val exchangeRateGateway: ExchangeRateGateway,
    private val eventPublisher: ApplicationEventPublisher,
) : ConvertCurrencyUseCase {

    override fun convert(
        userId: UUID,
        idempotencyKey: UUID,
        sourceCurrency: String,
        sourceAmount: java.math.BigDecimal,
        targetCurrency: String,
    ): Transaction {
        transactionRepository.findByIdempotencyKey(idempotencyKey)?.let { return it }

        val rate = exchangeRateGateway.getRate(sourceCurrency, targetCurrency)
        val targetAmount = sourceAmount.multiply(rate).setScale(4, RoundingMode.HALF_EVEN)

        val transaction = Transaction.create(
            id = UUID.randomUUID(),
            userId = userId,
            idempotencyKey = idempotencyKey,
            sourceCurrency = sourceCurrency,
            sourceAmount = sourceAmount,
            targetCurrency = targetCurrency,
            targetAmount = targetAmount,
            exchangeRate = rate,
        )
        val saved = transactionRepository.save(transaction)
        eventPublisher.publishEvent(TransactionCreatedEvent(saved))
        return saved
    }
}
