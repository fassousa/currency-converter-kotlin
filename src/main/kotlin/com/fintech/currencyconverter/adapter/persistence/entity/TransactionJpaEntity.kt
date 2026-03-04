package com.fintech.currencyconverter.adapter.persistence.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transactions")
class TransactionJpaEntity(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "idempotency_key", nullable = false, unique = true)
    val idempotencyKey: UUID,

    @Column(name = "source_currency", nullable = false, length = 3)
    val sourceCurrency: String,

    @Column(name = "source_amount", nullable = false, precision = 19, scale = 4)
    val sourceAmount: BigDecimal,

    @Column(name = "target_currency", nullable = false, length = 3)
    val targetCurrency: String,

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    val targetAmount: BigDecimal,

    @Column(name = "exchange_rate", nullable = false, precision = 27, scale = 8)
    val exchangeRate: BigDecimal,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
