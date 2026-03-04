package com.fintech.currencyconverter.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CurrencyTest {

    @Test
    fun `valid codes are accepted`() {
        assertDoesNotThrow { Currency("USD") }
        assertDoesNotThrow { Currency("EUR") }
        assertDoesNotThrow { Currency("GBP") }
    }

    @Test
    fun `invalid codes throw IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { Currency("us") }
        assertThrows<IllegalArgumentException> { Currency("USDD") }
        assertThrows<IllegalArgumentException> { Currency("123") }
        assertThrows<IllegalArgumentException> { Currency("") }
        assertThrows<IllegalArgumentException> { Currency("usd") }
    }

    @Test
    fun `USD toString returns USD`() {
        assertEquals("USD", Currency.USD.toString())
    }

    @Test
    fun `companion constants are correct`() {
        assertEquals("USD", Currency.USD.code)
        assertEquals("EUR", Currency.EUR.code)
        assertEquals("GBP", Currency.GBP.code)
        assertEquals("BRL", Currency.BRL.code)
    }
}

