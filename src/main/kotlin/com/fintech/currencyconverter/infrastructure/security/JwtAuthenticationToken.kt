package com.fintech.currencyconverter.infrastructure.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.util.Date
import java.util.UUID

class JwtAuthenticationToken(
    val userId: UUID,
    val jti: String,
    val expiration: Date,
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    init {
        super.setAuthenticated(authorities.isNotEmpty())
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): Any = userId
}

