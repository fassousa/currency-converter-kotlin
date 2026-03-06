package com.fintech.currencyconverter.adapter.persistence

import com.fintech.currencyconverter.adapter.persistence.mapper.TransactionMapper
import com.fintech.currencyconverter.adapter.persistence.repository.TransactionJpaRepository
import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class TransactionPersistenceAdapter(
    private val jpaRepository: TransactionJpaRepository,
    private val mapper: TransactionMapper,
) : TransactionRepository {

    @Transactional
    override fun save(transaction: Transaction): Transaction =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(transaction)))

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Transaction? =
        jpaRepository.findById(id).orElse(null)?.let(mapper::toDomain)

    @Transactional(readOnly = true)
    override fun findByIdempotencyKey(key: UUID): Transaction? =
        jpaRepository.findByIdempotencyKey(key)?.let(mapper::toDomain)

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: UUID, page: Int, size: Int): List<Transaction> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return jpaRepository.findAllByUserId(userId, pageable).map(mapper::toDomain)
    }

    @Transactional(readOnly = true)
    override fun findPageByUserId(userId: UUID, page: Int, size: Int): PageResult<Transaction> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val springPage = jpaRepository.findPageByUserId(userId, pageable)
        return PageResult.of(
            content = springPage.content.map(mapper::toDomain),
            page = page,
            size = size,
            totalElements = springPage.totalElements
        )
    }
}



