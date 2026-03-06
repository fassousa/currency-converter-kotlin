package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.model.Currency
import com.fintech.currencyconverter.domain.model.Money
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.UserId
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class TransactionHistoryServiceTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val service = TransactionHistoryService(transactionRepository)

    @Test
    fun `getHistory delegates to repository with correct pagination`() {
        val userId = UserId.generate()
        val tx = Transaction.create(userId, UUID.randomUUID(), Money(BigDecimal("50"), Currency.USD), Currency.EUR, BigDecimal("0.92"))
        every { transactionRepository.findAllByUserId(userId, 0, 10) } returns listOf(tx)

        val result = service.getHistory(userId, 0, 10)

        assertEquals(1, result.size)
        assertEquals(tx, result[0])
        verify { transactionRepository.findAllByUserId(userId, 0, 10) }
    }

    @Test
    fun `getHistory returns empty list when no transactions`() {
        val userId = UserId.generate()
        every { transactionRepository.findAllByUserId(userId, 0, 10) } returns emptyList()

        val result = service.getHistory(userId, 0, 10)

        assertTrue(result.isEmpty())
    }
}

