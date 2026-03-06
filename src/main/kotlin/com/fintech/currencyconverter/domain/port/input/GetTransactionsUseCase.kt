package com.fintech.currencyconverter.domain.port.input

import com.fintech.currencyconverter.domain.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface GetTransactionsUseCase {
    fun execute(userId: UUID, pageable: Pageable): Page<Transaction>
}

