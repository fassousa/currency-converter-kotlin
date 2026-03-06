package com.fintech.currencyconverter.infrastructure.config

import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import com.fintech.currencyconverter.domain.service.AuthenticateUserService
import com.fintech.currencyconverter.domain.service.CreateTransactionService
import com.fintech.currencyconverter.domain.service.GetTransactionsService
import com.fintech.currencyconverter.domain.service.RegisterUserService
import com.fintech.currencyconverter.infrastructure.security.BcryptPasswordHasher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Wires pure domain services with their infrastructure adapters.
 * Domain services carry zero Spring annotations — all framework plumbing lives here.
 * Transactional boundaries are enforced at the JPA repository adapter layer.
 */
@Configuration
class DomainServiceConfig {

    @Bean
    fun passwordHasher(): PasswordHasher = BcryptPasswordHasher(BCryptPasswordEncoder())

    @Bean
    fun registerUserService(
        userRepository: UserRepositoryPort,
        passwordHasher: PasswordHasher,
    ): RegisterUserService = RegisterUserService(userRepository, passwordHasher)

    @Bean
    fun authenticateUserService(
        userRepository: UserRepositoryPort,
        passwordHasher: PasswordHasher,
    ): AuthenticateUserService = AuthenticateUserService(userRepository, passwordHasher)

    @Bean
    fun createTransactionService(
        transactionRepository: TransactionRepositoryPort,
        exchangeRateGateway: ExchangeRateGateway,
    ): CreateTransactionService = CreateTransactionService(transactionRepository, exchangeRateGateway)

    @Bean
    fun getTransactionsService(
        transactionRepository: TransactionRepositoryPort,
    ): GetTransactionsService = GetTransactionsService(transactionRepository)
}



