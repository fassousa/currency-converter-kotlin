package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.application.event.TransactionCreatedEvent
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class CurrencyConversionServiceTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val exchangeRateGateway = mockk<ExchangeRateGateway>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service = CurrencyConversionService(transactionRepository, exchangeRateGateway, eventPublisher)

    private val userId = UUID.randomUUID()
    private val idempotencyKey = UUID.randomUUID()

    private fun buildTx(userId: UUID = this.userId, idempotencyKey: UUID = this.idempotencyKey) = Transaction(
        id = UUID.randomUUID(),
        userId = userId,
        idempotencyKey = idempotencyKey,
        sourceCurrency = "USD",
        sourceAmount = BigDecimal("100.00"),
        targetCurrency = "EUR",
        targetAmount = BigDecimal("92.0000"),
        exchangeRate = BigDecimal("0.92"),
        createdAt = OffsetDateTime.now(),
    )

    @BeforeEach
    fun setUp() { clearAllMocks() }

    @Test
    fun `convert returns existing transaction when idempotency key already exists`() {
        val existing = buildTx()
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns existing

        val result = service.convert(userId, idempotencyKey, "USD", BigDecimal("100.00"), "EUR")

        assertEquals(existing, result)
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }

    @Test
    fun `convert saves transaction and publishes event on success`() {
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null
        every { exchangeRateGateway.getRate("USD", "EUR") } returns BigDecimal("0.92")
        val savedTx = slot<Transaction>()
        every { transactionRepository.save(capture(savedTx)) } answers { savedTx.captured }

        val result = service.convert(userId, idempotencyKey, "USD", BigDecimal("100.00"), "EUR")

        assertEquals(userId, result.userId)
        assertEquals(idempotencyKey, result.idempotencyKey)
        assertEquals("USD", result.sourceCurrency)
        assertEquals("EUR", result.targetCurrency)
        verify { eventPublisher.publishEvent(ofType<TransactionCreatedEvent>()) }
    }
}
