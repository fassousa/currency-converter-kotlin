package com.fintech.currencyconverter.infrastructure.config

import com.fintech.currencyconverter.application.service.CurrencyConversionService
import com.fintech.currencyconverter.application.service.TransactionHistoryService
import com.fintech.currencyconverter.application.service.UserService
import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import com.fintech.currencyconverter.domain.service.AuthenticateUserService
import com.fintech.currencyconverter.domain.service.CreateTransactionService
import com.fintech.currencyconverter.domain.service.GetTransactionsService
import com.fintech.currencyconverter.domain.service.RegisterUserService
import com.fintech.currencyconverter.infrastructure.security.BcryptPasswordHasher
import com.fintech.currencyconverter.port.outbound.TransactionRepository as AppTransactionRepository
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway as AppExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Wires pure domain and application services with their infrastructure adapters.
 * All services carry zero Spring annotations — all framework plumbing lives here.
 * Transactional boundaries are enforced at the JPA repository adapter layer via @Transactional.
 */
@Configuration
class DomainServiceConfig {

    @Bean
    fun passwordHasher(): PasswordHasher = BcryptPasswordHasher(BCryptPasswordEncoder())

    // Domain services (pure, framework-free)
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

    // Application services (pure, framework-free)
    @Bean
    fun userService(
        userRepository: UserRepository,
        passwordHasher: PasswordHasher,
    ): UserService = UserService(userRepository, passwordHasher)

    @Bean
    fun currencyConversionService(
        transactionRepository: AppTransactionRepository,
        exchangeRateGateway: AppExchangeRateGateway,
        eventPublisher: ApplicationEventPublisher,
    ): CurrencyConversionService = CurrencyConversionService(
        transactionRepository,
        exchangeRateGateway,
        eventPublisher
    )

    @Bean
    fun transactionHistoryService(
        transactionRepository: AppTransactionRepository,
    ): TransactionHistoryService = TransactionHistoryService(transactionRepository)
}



