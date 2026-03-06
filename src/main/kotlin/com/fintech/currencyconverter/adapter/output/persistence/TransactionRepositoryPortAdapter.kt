package com.fintech.currencyconverter.adapter.output.persistence

import com.fintech.currencyconverter.adapter.persistence.mapper.TransactionMapper
import com.fintech.currencyconverter.adapter.persistence.repository.TransactionJpaRepository
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionRepositoryPortAdapter(
    private val jpaRepository: TransactionJpaRepository,
    private val mapper: TransactionMapper,
) : TransactionRepositoryPort {

    override fun save(transaction: Transaction): Transaction =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(transaction)))

    override fun findByUserId(userId: UUID, pageable: Pageable): Page<Transaction> =
        jpaRepository.findPageByUserId(userId, pageable).map(mapper::toDomain)
}

