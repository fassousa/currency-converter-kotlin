package com.fintech.currencyconverter.port.inbound

import com.fintech.currencyconverter.domain.model.Transaction
import com.fintech.currencyconverter.domain.model.UserId

interface GetTransactionHistoryUseCase {
    fun getHistory(userId: UserId, page: Int, size: Int): List<Transaction>
}
