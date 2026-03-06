package com.fintech.currencyconverter.adapter.input.rest.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SignInRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "must be a valid email address")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,
)

