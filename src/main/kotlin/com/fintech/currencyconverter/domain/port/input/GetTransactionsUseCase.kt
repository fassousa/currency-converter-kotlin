package com.fintech.currencyconverter.domain.port.input

import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.model.Transaction
import java.util.UUID

interface GetTransactionsUseCase {
    fun execute(userId: UUID, page: Int, size: Int): PageResult<Transaction>
}

