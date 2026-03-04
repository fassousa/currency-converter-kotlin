package com.fintech.currencyconverter.domain.model

import java.util.UUID

@JvmInline
value class TransactionId(val value: UUID) {
    companion object {
        fun generate(): TransactionId = TransactionId(UUID.randomUUID())
    }
}

