package com.fintech.currencyconverter.domain.model

import java.time.OffsetDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordDigest: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)


