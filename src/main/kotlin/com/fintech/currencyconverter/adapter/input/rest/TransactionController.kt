package com.fintech.currencyconverter.adapter.input.rest

import com.fintech.currencyconverter.adapter.input.rest.dto.CreateTransactionRequest
import com.fintech.currencyconverter.adapter.input.rest.dto.TransactionResponse
import com.fintech.currencyconverter.adapter.input.rest.mapper.toResponse
import com.fintech.currencyconverter.domain.port.input.CreateTransactionCommand
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/transactions")
class TransactionController(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
) {
    @PostMapping
    fun createTransaction(
        @RequestHeader("Idempotency-Key") idempotencyKey: UUID,
        @Valid @RequestBody request: CreateTransactionRequest,
        authentication: Authentication,
    ): ResponseEntity<TransactionResponse> {
        val jwtAuth = authentication as JwtAuthenticationToken
        val command = CreateTransactionCommand(
            userId = jwtAuth.userId,
            idempotencyKey = idempotencyKey,
            sourceCurrency = request.sourceCurrency!!,
            sourceAmount = request.sourceAmount!!,
            targetCurrency = request.targetCurrency!!,
        )
        val transaction = createTransactionUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction.toResponse())
    }

    @GetMapping
    fun getTransactions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): ResponseEntity<Page<TransactionResponse>> {
        val jwtAuth = authentication as JwtAuthenticationToken
        val clampedSize = size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val clampedPage = page.coerceAtLeast(0)
        val pageable = PageRequest.of(clampedPage, clampedSize, Sort.by("createdAt").descending())
        val transactions = getTransactionsUseCase.execute(jwtAuth.userId, pageable)
        return ResponseEntity.ok(transactions.map { it.toResponse() })
    }

    companion object {
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 100
    }
}
