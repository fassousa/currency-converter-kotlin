package com.fintech.currencyconverter.adapter.output.persistence

import com.fintech.currencyconverter.adapter.persistence.mapper.TransactionMapper
import com.fintech.currencyconverter.adapter.persistence.repository.TransactionJpaRepository
import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionRepositoryPortAdapter(
    private val jpaRepository: TransactionJpaRepository,
    private val mapper: TransactionMapper,
) : TransactionRepositoryPort {

    override fun save(transaction: Transaction): Transaction =
        mapper.toDomain(jpaRepository.save(mapper.toEntity(transaction)))

    override fun findByIdempotencyKey(key: UUID): Transaction? =
        jpaRepository.findByIdempotencyKey(key)?.let(mapper::toDomain)

    override fun findByUserId(userId: UUID, page: Int, size: Int): PageResult<Transaction> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val springPage = jpaRepository.findPageByUserId(userId, pageable)
        return PageResult.of(
            content = springPage.content.map(mapper::toDomain),
            page = springPage.number,
            size = springPage.size,
            totalElements = springPage.totalElements,
        )
    }
}

