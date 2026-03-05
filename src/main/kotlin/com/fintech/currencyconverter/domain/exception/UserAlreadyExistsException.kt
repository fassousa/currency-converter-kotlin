package com.fintech.currencyconverter.domain.exception

class UserAlreadyExistsException(email: String) :
    RuntimeException("User already exists: $email")

