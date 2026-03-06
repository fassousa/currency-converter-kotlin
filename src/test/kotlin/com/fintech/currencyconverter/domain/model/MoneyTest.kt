package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `positive amount is accepted`() {
        assertDoesNotThrow { Money(BigDecimal("10.00"), Currency.USD) }
    }

    @Test
    fun `zero amount throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { Money(BigDecimal.ZERO, Currency.USD) }
    }

    @Test
    fun `negative amount throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { Money(BigDecimal("-1.00"), Currency.USD) }
    }

    @Test
    fun `plus works for same currency`() {
        val a = Money(BigDecimal("10.00"), Currency.USD)
        val b = Money(BigDecimal("5.00"), Currency.USD)
        val result = a + b
        assertEquals(BigDecimal("15.00"), result.amount)
        assertEquals(Currency.USD, result.currency)
    }

    @Test
    fun `plus throws for different currencies`() {
        val a = Money(BigDecimal("10.00"), Currency.USD)
        val b = Money(BigDecimal("5.00"), Currency.EUR)
        assertThrows<IllegalArgumentException> { a + b }
    }

    @Test
    fun `convertTo returns correct result`() {
        val money = Money(BigDecimal("100.00"), Currency.USD)
        val result = money.convertTo(Currency.EUR, BigDecimal("0.92"))
        assertEquals(BigDecimal("92.0000"), result.amount)
        assertEquals(Currency.EUR, result.currency)
    }

    @Test
    fun `convertTo rounds to 4 decimal places with HALF_EVEN (Banker's Rounding)`() {
        // 1.00 * 0.12345 = 0.12345 — the digit before the 5 is 4 (even), so HALF_EVEN rounds DOWN to 0.1234
        val money = Money(BigDecimal("1.00"), Currency.USD)
        val result = money.convertTo(Currency.EUR, BigDecimal("0.12345"))
        assertEquals(BigDecimal("0.1234"), result.amount)
    }

    @Test
    fun `convertTo rounds half-up when preceding digit is odd (HALF_EVEN)`() {
        // 1.00 * 0.12355 = 0.12355 — the digit before the 5 is 5 (odd), so HALF_EVEN rounds UP to 0.1236
        val money = Money(BigDecimal("1.00"), Currency.USD)
        val result = money.convertTo(Currency.EUR, BigDecimal("0.12355"))
        assertEquals(BigDecimal("0.1236"), result.amount)
    }
}

