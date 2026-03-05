package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionHistoryServiceTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionHistoryService(transactionRepository)

    private fun buildTx(userId: UUID) = Transaction(
        id = UUID.randomUUID(),
        userId = userId,
        idempotencyKey = UUID.randomUUID(),
        sourceCurrency = "USD",
        sourceAmount = BigDecimal("50"),
        targetCurrency = "EUR",
        targetAmount = BigDecimal("46"),
        exchangeRate = BigDecimal("0.92"),
        createdAt = OffsetDateTime.now(),
    )

    @Test
    fun `getHistory delegates to repository with correct pagination`() {
        val userId = UUID.randomUUID()
        val tx = buildTx(userId)
        every { transactionRepository.findAllByUserId(userId, 0, 10) } returns listOf(tx)

        val result = service.getHistory(userId, 0, 10)

        assertEquals(1, result.size)
        assertEquals(tx, result[0])
        verify { transactionRepository.findAllByUserId(userId, 0, 10) }
    }

    @Test
    fun `getHistory returns empty list when no transactions`() {
        val userId = UUID.randomUUID()
        every { transactionRepository.findAllByUserId(userId, 0, 10) } returns emptyList()

        val result = service.getHistory(userId, 0, 10)

        assertTrue(result.isEmpty())
    }
}
