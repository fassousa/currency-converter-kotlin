package com.fintech.currencyconverter.adapter.input.rest.exception

import com.fintech.currencyconverter.adapter.input.rest.dto.ErrorResponse
import com.fintech.currencyconverter.adapter.input.rest.dto.FieldError
import com.fintech.currencyconverter.adapter.input.rest.dto.ValidationErrorResponse
import com.fintech.currencyconverter.domain.exception.CurrencyNotSupportedException
import com.fintech.currencyconverter.domain.exception.ExchangeRateUnavailableException
import com.fintech.currencyconverter.domain.exception.InvalidCredentialsException
import com.fintech.currencyconverter.domain.exception.UserAlreadyExistsException
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CurrencyNotSupportedException::class)
    fun handleCurrencyNotSupported(e: CurrencyNotSupportedException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .unprocessableEntity()
            .body(ErrorResponse("CURRENCY_NOT_SUPPORTED", e.message ?: "Currency not supported"))

    @ExceptionHandler(ExchangeRateUnavailableException::class)
    fun handleExchangeRateUnavailable(e: ExchangeRateUnavailableException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse("SERVICE_UNAVAILABLE", e.message ?: "Exchange rate service unavailable"))

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(e: UserAlreadyExistsException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse("USER_ALREADY_EXISTS", e.message ?: "User already exists"))

    @ExceptionHandler(UserNotFoundException::class, InvalidCredentialsException::class)
    fun handleAuthFailure(e: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("INVALID_CREDENTIALS", "Invalid credentials"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map {
            FieldError(it.field, it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity
            .unprocessableEntity()
            .body(ValidationErrorResponse("VALIDATION_ERROR", errors))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        val message = (e.cause?.message ?: e.message ?: "").lowercase()
        return if (message.contains("idempotency_key")) {
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse("IDEMPOTENCY_CONFLICT", "Duplicate request: idempotency key already used"))
        } else {
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse("DATA_CONFLICT", "Data conflict"))
        }
    }
}

