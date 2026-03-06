package com.fintech.currencyconverter.adapter.persistence.mapper

import com.fintech.currencyconverter.adapter.persistence.entity.TransactionJpaEntity
import com.fintech.currencyconverter.domain.model.Transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
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
            exchangeRate = BigDecimal("0.91500000"), createdAt = now,
        )

        val domain = mapper.toDomain(entity)

        assertEquals(id, domain.id)
        assertEquals(userId, domain.userId)
        assertEquals(idKey, domain.idempotencyKey)
        assertEquals("USD", domain.sourceCurrency)
        assertEquals(BigDecimal("100.0000"), domain.sourceAmount)
        assertEquals("EUR", domain.targetCurrency)
        assertEquals(BigDecimal("91.5000"), domain.targetAmount)
        assertEquals(BigDecimal("0.91500000"), domain.exchangeRate)
        assertEquals(now.atOffset(ZoneOffset.UTC), domain.createdAt)
    }

    @Test
    fun `toEntity maps all fields correctly`() {
        val tx = Transaction(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            sourceCurrency = "USD",
            sourceAmount = BigDecimal("200.00"),
            targetCurrency = "EUR",
            targetAmount = BigDecimal("184.00"),
            exchangeRate = BigDecimal("0.92"),
        )

        val entity = mapper.toEntity(tx)

        assertEquals(tx.id, entity.id)
        assertEquals(tx.userId, entity.userId)
        assertEquals("USD", entity.sourceCurrency)
        assertEquals("EUR", entity.targetCurrency)
        assertEquals(tx.exchangeRate, entity.exchangeRate)
        assertEquals(tx.sourceAmount, entity.sourceAmount)
        assertEquals(tx.targetAmount, entity.targetAmount)
    }

    @Test
    fun `round-trip toDomain then toEntity preserves identity`() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val idKey = UUID.randomUUID()
        val now = Instant.now()
        val entity = TransactionJpaEntity(
            id = id, userId = userId, idempotencyKey = idKey,
            sourceCurrency = "USD", sourceAmount = BigDecimal("50.0000"),
            targetCurrency = "BRL", targetAmount = BigDecimal("250.0000"),
            exchangeRate = BigDecimal("5.00000000"), createdAt = now,
        )

        val roundTrip = mapper.toEntity(mapper.toDomain(entity))

        assertEquals(entity.id, roundTrip.id)
        assertEquals(entity.userId, roundTrip.userId)
        assertEquals(entity.idempotencyKey, roundTrip.idempotencyKey)
        assertEquals(entity.sourceCurrency, roundTrip.sourceCurrency)
        assertEquals(entity.sourceAmount, roundTrip.sourceAmount)
        assertEquals(entity.targetCurrency, roundTrip.targetCurrency)
        assertEquals(entity.targetAmount, roundTrip.targetAmount)
        assertEquals(entity.exchangeRate, roundTrip.exchangeRate)
    }
}
