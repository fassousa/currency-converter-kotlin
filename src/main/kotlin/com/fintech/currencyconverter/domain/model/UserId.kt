package com.fintech.currencyconverter.domain.model

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
    }
}

