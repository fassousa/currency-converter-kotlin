package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.*
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
    fun `convertTo rounds to 4 decimal places with HALF_UP`() {
        val money = Money(BigDecimal("1.00"), Currency.USD)
        val result = money.convertTo(Currency.EUR, BigDecimal("0.123456"))
        assertEquals(BigDecimal("0.1235"), result.amount)
    }
}
