package com.fintech.currencyconverter.port.inbound

import com.fintech.currencyconverter.domain.model.Transaction
import java.util.UUID

interface GetTransactionHistoryUseCase {
    fun getHistory(userId: UUID, page: Int, size: Int): List<Transaction>
}
