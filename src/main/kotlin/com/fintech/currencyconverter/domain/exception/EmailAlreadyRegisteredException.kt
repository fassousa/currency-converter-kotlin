package com.fintech.currencyconverter.domain.exception

class EmailAlreadyRegisteredException(email: String) : DomainException("Email already registered: $email")

