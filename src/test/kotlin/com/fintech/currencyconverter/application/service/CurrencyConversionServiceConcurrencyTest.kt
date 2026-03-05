package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.application.event.TransactionCreatedEvent
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class CurrencyConversionServiceConcurrencyTest {

    @Test
    fun `idempotent convert under concurrent load returns same transaction for all callers`() {
        val transactionRepository = mockk<TransactionRepository>()
        val exchangeRateGateway = mockk<ExchangeRateGateway>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service = CurrencyConversionService(transactionRepository, exchangeRateGateway, eventPublisher)

        val userId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()

        val canonicalTx = Transaction(
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

        val saveCount = AtomicInteger(0)
        val findCallCount = AtomicInteger(0)

        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } answers {
            if (findCallCount.getAndIncrement() == 0) null else canonicalTx
        }
        every { exchangeRateGateway.getRate("USD", "EUR") } returns BigDecimal("0.92")
        every { transactionRepository.save(any()) } answers {
            saveCount.incrementAndGet()
            canonicalTx
        }

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = ConcurrentHashMap<Int, Transaction>()
        val caughtErrors = ConcurrentHashMap<Int, Throwable>()

        repeat(threadCount) { i ->
            executor.submit {
                try {
                    results[i] = service.convert(userId, idempotencyKey, "USD", BigDecimal("100.00"), "EUR")
                } catch (e: Exception) {
                    caughtErrors[i] = e
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertEquals(0, caughtErrors.size, "No thread should have thrown an exception: $caughtErrors")
        assertEquals(threadCount, results.size)
        val distinctIds = results.values.map { it.id }.toSet()
        assertEquals(1, distinctIds.size, "All concurrent callers must receive the same transaction")
        assertEquals(1, saveCount.get(), "Transaction should be saved exactly once")
    }
}
