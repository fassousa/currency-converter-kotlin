package com.fintech.currencyconverter.adapter.persistence.repository

import com.fintech.currencyconverter.adapter.persistence.entity.TransactionJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionJpaRepository : JpaRepository<TransactionJpaEntity, UUID> {
    fun findByIdempotencyKey(key: UUID): TransactionJpaEntity?
    fun findAllByUserId(userId: UUID, pageable: Pageable): List<TransactionJpaEntity>
}

