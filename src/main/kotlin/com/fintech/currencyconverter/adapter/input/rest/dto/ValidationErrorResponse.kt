package com.fintech.currencyconverter.adapter.input.rest.dto

data class FieldError(val field: String, val message: String)

data class ValidationErrorResponse(
    val code: String,
    val errors: List<FieldError>,
)

