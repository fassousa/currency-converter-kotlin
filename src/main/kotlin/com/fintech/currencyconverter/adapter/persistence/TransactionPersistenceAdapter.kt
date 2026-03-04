package com.fintech.currencyconverter.adapter.persistence

import com.fintech.currencyconverter.adapter.persistence.mapper.TransactionMapper
import com.fintech.currencyconverter.adapter.persistence.repository.TransactionJpaRepository
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.TransactionId
import com.fintech.currencyconverter.domain.model.UserId
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionPersistenceAdapter(
    private val jpaRepository: TransactionJpaRepository,
    private val mapper: TransactionMapper
) : TransactionRepository {

    override fun save(transaction: Transaction): Transaction =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(transaction)))

    override fun findById(id: TransactionId): Transaction? =
        jpaRepository.findById(id.value).orElse(null)?.let(mapper::toDomain)

    override fun findByIdempotencyKey(key: UUID): Transaction? =
        jpaRepository.findByIdempotencyKey(key)?.let(mapper::toDomain)

    override fun findAllByUserId(userId: UserId, page: Int, size: Int): List<Transaction> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return jpaRepository.findAllByUserId(userId.value, pageable).map(mapper::toDomain)
    }
}

