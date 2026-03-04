package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class TransactionTest {

    private val userId = UserId.generate()
    private val idempotencyKey = UUID.randomUUID()
    private val sourceMoney = Money(BigDecimal("100.00"), Currency.USD)

    @Test
    fun `create with valid data succeeds`() {
        val tx = Transaction.create(userId, idempotencyKey, sourceMoney, Currency.EUR, BigDecimal("0.92"))
        assertEquals(userId, tx.userId)
        assertEquals(idempotencyKey, tx.idempotencyKey)
        assertEquals(sourceMoney, tx.sourceMoney)
        assertEquals(Currency.EUR, tx.targetMoney.currency)
    }

    @Test
    fun `same source and target currency throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(userId, idempotencyKey, sourceMoney, Currency.USD, BigDecimal("1.00"))
        }
    }

    @Test
    fun `non-positive exchange rate throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            Transaction.create(userId, idempotencyKey, sourceMoney, Currency.EUR, BigDecimal.ZERO)
        }
        assertThrows<IllegalArgumentException> {
            Transaction.create(userId, idempotencyKey, sourceMoney, Currency.EUR, BigDecimal("-0.5"))
        }
    }

    @Test
    fun `targetMoney amount is sourceMoney amount times rate rounded to 4dp HALF_UP`() {
        val tx = Transaction.create(userId, idempotencyKey, sourceMoney, Currency.EUR, BigDecimal("0.92345"))
        assertEquals(BigDecimal("92.3450"), tx.targetMoney.amount)
    }
}
