package com.fintech.currencyconverter.domain.model

import com.fintech.currencyconverter.domain.exception.InvalidAmountException
import com.fintech.currencyconverter.domain.exception.InvalidCurrencyException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionTest {

    private val userId = UUID.randomUUID()
    private val idempotencyKey = UUID.randomUUID()

    @Test
    fun `create() with valid data stores correct fields`() {
        val tx = Transaction.create(
            id = UUID.randomUUID(),
            userId = userId,
            idempotencyKey = idempotencyKey,
            sourceCurrency = "USD",
            sourceAmount = BigDecimal("100.00"),
            targetCurrency = "EUR",
            targetAmount = BigDecimal("92.3450"),
            exchangeRate = BigDecimal("0.92345"),
        )
        assertEquals(userId, tx.userId)
        assertEquals(idempotencyKey, tx.idempotencyKey)
        assertEquals("USD", tx.sourceCurrency)
        assertEquals("EUR", tx.targetCurrency)
        assertEquals(BigDecimal("92.3450"), tx.targetAmount)
    }

    @Test
    fun `two transactions with same id are equal`() {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val t1 = Transaction(id, userId, idempotencyKey, "USD", BigDecimal("100"), "EUR", BigDecimal("90"), BigDecimal("0.9"), now)
        val t2 = Transaction(id, userId, idempotencyKey, "USD", BigDecimal("100"), "EUR", BigDecimal("90"), BigDecimal("0.9"), now)
        assertEquals(t1, t2)
    }

    @Test
    fun `create() rejects non-positive sourceAmount`() {
        assertThrows<InvalidAmountException> {
            Transaction.create(
                id = UUID.randomUUID(), userId = userId, idempotencyKey = idempotencyKey,
                sourceCurrency = "USD", sourceAmount = BigDecimal.ZERO,
                targetCurrency = "EUR", targetAmount = BigDecimal("0"),
                exchangeRate = BigDecimal("0.92"),
            )
        }
    }

    @Test
    fun `create() rejects non-positive exchangeRate`() {
        assertThrows<InvalidAmountException> {
            Transaction.create(
                id = UUID.randomUUID(), userId = userId, idempotencyKey = idempotencyKey,
                sourceCurrency = "USD", sourceAmount = BigDecimal("100"),
                targetCurrency = "EUR", targetAmount = BigDecimal("92"),
                exchangeRate = BigDecimal("-0.1"),
            )
        }
    }

    @Test
    fun `create() rejects invalid currency code format`() {
        assertThrows<InvalidCurrencyException> {
            Transaction.create(
                id = UUID.randomUUID(), userId = userId, idempotencyKey = idempotencyKey,
                sourceCurrency = "us", sourceAmount = BigDecimal("100"),
                targetCurrency = "EUR", targetAmount = BigDecimal("92"),
                exchangeRate = BigDecimal("0.92"),
            )
        }
    }

    @Test
    fun `create() rejects same source and target currency`() {
        assertThrows<InvalidCurrencyException> {
            Transaction.create(
                id = UUID.randomUUID(), userId = userId, idempotencyKey = idempotencyKey,
                sourceCurrency = "USD", sourceAmount = BigDecimal("100"),
                targetCurrency = "USD", targetAmount = BigDecimal("100"),
                exchangeRate = BigDecimal("1.0"),
            )
        }
    }
}
