package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class TransactionTest {

    private val userId = UUID.randomUUID()
    private val idempotencyKey = UUID.randomUUID()

    @Test
    fun `create transaction with valid data stores correct fields`() {
        val tx = Transaction(
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
        val now = java.time.OffsetDateTime.now()
        val t1 = Transaction(id, userId, idempotencyKey, "USD", BigDecimal("100"), "EUR", BigDecimal("90"), BigDecimal("0.9"), now)
        val t2 = Transaction(id, userId, idempotencyKey, "USD", BigDecimal("100"), "EUR", BigDecimal("90"), BigDecimal("0.9"), now)
        assertEquals(t1, t2)
    }

    @Test
    fun `factory method create returns valid transaction`() {
        val tx = Transaction.create(
            id = UUID.randomUUID(),
            userId = userId,
            idempotencyKey = idempotencyKey,
            sourceCurrency = "USD",
            sourceAmount = BigDecimal("100.00"),
            targetCurrency = "EUR",
            targetAmount = BigDecimal("92.00"),
            exchangeRate = BigDecimal("0.92"),
        )
        assertEquals("USD", tx.sourceCurrency)
        assertEquals("EUR", tx.targetCurrency)
    }

    @Test
    fun `factory method rejects non-uppercase source currency`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(
                id = UUID.randomUUID(),
                userId = userId,
                idempotencyKey = idempotencyKey,
                sourceCurrency = "usd",
                sourceAmount = BigDecimal("100.00"),
                targetCurrency = "EUR",
                targetAmount = BigDecimal("92.00"),
                exchangeRate = BigDecimal("0.92"),
            )
        }
    }

    @Test
    fun `factory method rejects same source and target currency`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(
                id = UUID.randomUUID(),
                userId = userId,
                idempotencyKey = idempotencyKey,
                sourceCurrency = "USD",
                sourceAmount = BigDecimal("100.00"),
                targetCurrency = "USD",
                targetAmount = BigDecimal("100.00"),
                exchangeRate = BigDecimal("1.00"),
            )
        }
    }

    @Test
    fun `factory method rejects non-positive source amount`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(
                id = UUID.randomUUID(),
                userId = userId,
                idempotencyKey = idempotencyKey,
                sourceCurrency = "USD",
                sourceAmount = BigDecimal("0"),
                targetCurrency = "EUR",
                targetAmount = BigDecimal("0"),
                exchangeRate = BigDecimal("0.92"),
            )
        }
    }

    @Test
    fun `factory method rejects non-positive exchange rate`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(
                id = UUID.randomUUID(),
                userId = userId,
                idempotencyKey = idempotencyKey,
                sourceCurrency = "USD",
                sourceAmount = BigDecimal("100.00"),
                targetCurrency = "EUR",
                targetAmount = BigDecimal("92.00"),
                exchangeRate = BigDecimal("-0.92"),
            )
        }
    }
}
