package com.fintech.currencyconverter.domain.exception

import com.fintech.currencyconverter.domain.model.UserId

class UserNotFoundException(id: UserId) : DomainException("User not found: ${id.value}")
