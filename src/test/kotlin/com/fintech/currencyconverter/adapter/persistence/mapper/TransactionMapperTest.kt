package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.TransactionJpaEntity
import com.fintech.currencyconverter.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class TransactionMapperTest {
    private val mapper = TransactionMapper()

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val idKey = UUID.randomUUID()
        val now = Instant.now()
        val entity = TransactionJpaEntity(
            id = id, userId = userId, idempotencyKey = idKey,
            sourceCurrency = "USD", sourceAmount = BigDecimal("100.0000"),
            targetCurrency = "EUR", targetAmount = BigDecimal("91.5000"),
            exchangeRate = BigDecimal("0.91500000"), createdAt = now
        )
        val domain = mapper.toDomain(entity)
        assertEquals(TransactionId(id), domain.id)
        assertEquals(UserId(userId), domain.userId)
        assertEquals(idKey, domain.idempotencyKey)
        assertEquals(Currency.USD, domain.sourceMoney.currency)
        assertEquals(BigDecimal("100.0000"), domain.sourceMoney.amount)
        assertEquals(Currency.EUR, domain.targetMoney.currency)
        assertEquals(now, domain.createdAt)
    }

    @Test
    fun `toEntity maps all fields correctly`() {
        val tx = Transaction.create(
            userId = UserId.generate(),
            idempotencyKey = UUID.randomUUID(),
            sourceMoney = Money(BigDecimal("200.00"), Currency.USD),
            targetCurrency = Currency.EUR,
            exchangeRate = BigDecimal("0.92")
        )
        val entity = mapper.toEntity(tx)
        assertEquals(tx.id.value, entity.id)
        assertEquals(tx.userId.value, entity.userId)
        assertEquals("USD", entity.sourceCurrency)
        assertEquals("EUR", entity.targetCurrency)
        assertEquals(tx.exchangeRate, entity.exchangeRate)
    }
}
