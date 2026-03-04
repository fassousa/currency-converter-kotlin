package com.fintech.currencyconverter.domain.exception

import java.util.UUID

class DuplicateTransactionException(idempotencyKey: UUID) : DomainException("Duplicate transaction: $idempotencyKey")
