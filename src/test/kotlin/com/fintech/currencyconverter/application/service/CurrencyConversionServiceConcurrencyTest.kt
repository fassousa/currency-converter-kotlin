package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.application.event.TransactionCreatedEvent
import com.fintech.currencyconverter.domain.model.Currency
import com.fintech.currencyconverter.domain.model.Money
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.domain.model.UserId
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import com.fintech.currencyconverter.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Idempotency concurrency test.
 *
 * Simulates N threads racing to convert with the same idempotency key.
 * The mock is configured so the first thread to call findByIdempotencyKey
 * sees null (no existing tx) and saves the transaction; every subsequent
 * call is served the already-saved transaction — exactly as the database
 * unique constraint + service logic would behave.
 */
class CurrencyConversionServiceConcurrencyTest {

    @Test
    fun `idempotent convert under concurrent load returns same transaction for all callers`() {
        val userRepository = mockk<UserRepository>()
        val transactionRepository = mockk<TransactionRepository>()
        val exchangeRateGateway = mockk<ExchangeRateGateway>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val service = CurrencyConversionService(
            userRepository, transactionRepository, exchangeRateGateway, eventPublisher
        )

        val userId = UserId.generate()
        val idempotencyKey = UUID.randomUUID()
        val sourceMoney = Money(BigDecimal("100.00"), Currency.USD)
        val user = User.create("concurrent@example.com", "hash")

        // The single canonical transaction that would be saved by the first winner
        val canonicalTx = Transaction.create(userId, idempotencyKey, sourceMoney, Currency.EUR, BigDecimal("0.92"))

        // save count — only one thread should trigger a real save
        val saveCount = AtomicInteger(0)

        // First call: returns null (no existing tx); every subsequent call returns the canonical one
        val findCallCount = AtomicInteger(0)
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } answers {
            if (findCallCount.getAndIncrement() == 0) null else canonicalTx
        }
        every { userRepository.findById(userId) } returns user
        every { exchangeRateGateway.getRate(Currency.USD, Currency.EUR) } returns BigDecimal("0.92")
        every { transactionRepository.save(any()) } answers {
            saveCount.incrementAndGet()
            canonicalTx
        }

        // Launch 10 concurrent callers
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = ConcurrentHashMap<Int, Transaction>()
        val errors = AtomicInteger(0)

        repeat(threadCount) { i ->
            executor.submit {
                try {
                    results[i] = service.convert(userId, idempotencyKey, sourceMoney, Currency.EUR)
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // No errors
        assertEquals(0, errors.get(), "No thread should have thrown an exception")

        // All 10 threads got a result
        assertEquals(threadCount, results.size)

        // All results refer to the same transaction
        val distinctIds = results.values.map { it.id }.toSet()
        assertEquals(1, distinctIds.size, "All concurrent callers must receive the same transaction")

        // Save was called exactly once (only the winning thread persisted)
        assertEquals(1, saveCount.get(), "Transaction should be saved exactly once")
    }
}


