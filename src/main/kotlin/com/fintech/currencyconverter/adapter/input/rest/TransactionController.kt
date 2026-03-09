package com.fintech.currencyconverter.adapter.input.rest

import com.fintech.currencyconverter.adapter.input.rest.dto.CreateTransactionRequest
import com.fintech.currencyconverter.adapter.input.rest.dto.TransactionResponse
import com.fintech.currencyconverter.adapter.input.rest.mapper.toResponse
import com.fintech.currencyconverter.domain.model.PageResult
import com.fintech.currencyconverter.domain.port.input.CreateTransactionCommand
import com.fintech.currencyconverter.domain.port.input.CreateTransactionUseCase
import com.fintech.currencyconverter.domain.port.input.GetTransactionsUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
@Tag(name = "Transactions", description = "Currency conversion transaction endpoints")
@SecurityRequirement(name = "bearerAuth")
class TransactionController(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
) {
    @PostMapping
    @Operation(
        summary = "Create a currency conversion transaction",
        description = "Converts an amount from one currency to another. Idempotency-Key header " +
            "is optional - server will auto-generate if not provided. Always returns the " +
            "idempotencyKey in response.",
    )
    fun createTransaction(
        @RequestHeader("Idempotency-Key", required = false) idempotencyKey: UUID?,
        @Valid @RequestBody request: CreateTransactionRequest,
        authentication: Authentication,
    ): ResponseEntity<TransactionResponse> {
        val jwtAuth = authentication as JwtAuthenticationToken
        val key = idempotencyKey ?: UUID.randomUUID()
        val command = CreateTransactionCommand(
            userId = jwtAuth.userId,
            idempotencyKey = key,
            sourceCurrency = request.sourceCurrency!!,
            sourceAmount = request.sourceAmount!!,
            targetCurrency = request.targetCurrency!!,
        )
        val transaction = createTransactionUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction.toResponse())
    }

    @GetMapping
    @Operation(
        summary = "List transactions",
        description = "Returns a paginated list of currency conversion transactions for the authenticated user",
    )
    fun getTransactions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): ResponseEntity<PageResult<TransactionResponse>> {
        val jwtAuth = authentication as JwtAuthenticationToken
        val clampedSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val clampedPage = page.coerceAtLeast(0)
        val result = getTransactionsUseCase.execute(jwtAuth.userId, clampedPage, clampedSize)
        return ResponseEntity.ok(PageResult(
            content = result.content.map { it.toResponse() },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        ))
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}

