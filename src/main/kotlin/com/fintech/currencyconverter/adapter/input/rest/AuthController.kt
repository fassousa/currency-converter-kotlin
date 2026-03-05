package com.fintech.currencyconverter.adapter.input.rest

import com.fintech.currencyconverter.adapter.input.rest.dto.AuthResponse
import com.fintech.currencyconverter.adapter.input.rest.dto.SignInRequest
import com.fintech.currencyconverter.adapter.input.rest.dto.SignUpRequest
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserCommand
import com.fintech.currencyconverter.domain.port.input.AuthenticateUserUseCase
import com.fintech.currencyconverter.domain.port.input.RegisterUserCommand
import com.fintech.currencyconverter.domain.port.input.RegisterUserUseCase
import com.fintech.currencyconverter.infrastructure.security.JwtAuthenticationToken
import com.fintech.currencyconverter.infrastructure.security.JwtService
import com.fintech.currencyconverter.infrastructure.security.TokenRevocationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/auth")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val jwtService: JwtService,
    private val tokenRevocationService: TokenRevocationService,
) {
    @PostMapping("/sign_up")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<AuthResponse> {
        val user = registerUserUseCase.execute(RegisterUserCommand(request.email, request.password))
        val token = jwtService.generateToken(user.id, user.email)
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse(token))
    }

    @PostMapping("/sign_in")
    fun signIn(@Valid @RequestBody request: SignInRequest): ResponseEntity<AuthResponse> {
        val user = authenticateUserUseCase.execute(AuthenticateUserCommand(request.email, request.password))
        val token = jwtService.generateToken(user.id, user.email)
        return ResponseEntity.ok(AuthResponse(token))
    }

    @PostMapping("/sign_out")
    fun signOut(authentication: Authentication): ResponseEntity<Unit> {
        val jwtAuth = authentication as JwtAuthenticationToken
        // Ensure at least a 1-second TTL so Redis doesn't reject the SET command
        val remainingMs = (jwtAuth.expiration.time - System.currentTimeMillis()).coerceAtLeast(MIN_REVOCATION_TTL_MS)
        tokenRevocationService.revoke(jwtAuth.jti, Duration.ofMillis(remainingMs))
        return ResponseEntity.noContent().build()
    }

    companion object {
        private const val MIN_REVOCATION_TTL_MS = 1_000L
    }
}
