package com.fintech.currencyconverter.infrastructure.config

import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import com.fintech.currencyconverter.domain.port.output.PasswordHasher
import com.fintech.currencyconverter.domain.port.output.TransactionRepositoryPort
import com.fintech.currencyconverter.domain.port.output.UserRepositoryPort
import com.fintech.currencyconverter.domain.service.AuthenticateUserService
import com.fintech.currencyconverter.domain.service.CreateTransactionService
import com.fintech.currencyconverter.domain.service.GetTransactionsService
import com.fintech.currencyconverter.domain.service.RegisterUserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional

@Configuration
class DomainServiceConfig {

    @Bean
    @Transactional
    fun createTransactionService(
        transactionRepository: TransactionRepositoryPort,
        exchangeRateGateway: ExchangeRateGateway,
    ): CreateTransactionService = CreateTransactionService(transactionRepository, exchangeRateGateway)

    @Bean
    fun authenticateUserService(
        userRepository: UserRepositoryPort,
        passwordHasher: PasswordHasher,
    ): AuthenticateUserService = AuthenticateUserService(userRepository, passwordHasher)

    @Bean
    @Transactional
    fun registerUserService(
        userRepository: UserRepositoryPort,
        passwordHasher: PasswordHasher,
    ): RegisterUserService = RegisterUserService(userRepository, passwordHasher)

    @Bean
    fun getTransactionsService(
        transactionRepository: TransactionRepositoryPort,
    ): GetTransactionsService = GetTransactionsService(transactionRepository)
}
