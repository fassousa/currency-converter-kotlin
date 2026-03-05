package com.fintech.currencyconverter.domain.exception

class UserNotFoundException(email: String) :
    RuntimeException("User not found: $email")

