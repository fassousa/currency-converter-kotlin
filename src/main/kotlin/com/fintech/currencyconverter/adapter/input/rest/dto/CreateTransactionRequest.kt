package com.fintech.currencyconverter.adapter.input.rest.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

data class CreateTransactionRequest(
    @field:NotBlank(message = "Source currency is required")
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "must be exactly 3 uppercase letters")
    val sourceCurrency: String?,

    @field:NotNull(message = "Source amount is required")
    @field:DecimalMin(value = "0.01", message = "must be greater than 0")
    val sourceAmount: BigDecimal?,

    @field:NotBlank(message = "Target currency is required")
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "must be exactly 3 uppercase letters")
    val targetCurrency: String?,
)
