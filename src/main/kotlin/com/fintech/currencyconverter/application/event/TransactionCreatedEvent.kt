package com.fintech.currencyconverter.application.event

import com.fintech.currencyconverter.domain.model.Transaction

data class TransactionCreatedEvent(val transaction: Transaction)
