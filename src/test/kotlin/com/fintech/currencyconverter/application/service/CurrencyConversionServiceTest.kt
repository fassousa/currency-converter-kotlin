package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.application.event.TransactionCreatedEvent
import com.fintech.currencyconverter.domain.exception.UserNotFoundException
import com.fintech.currencyconverter.domain.model.*
import com.fintech.currencyconverter.port.outbound.ExchangeRateGateway
import com.fintech.currencyconverter.port.outbound.TransactionRepository
import com.fintech.currencyconverter.port.outbound.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.UUID

class CurrencyConversionServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val exchangeRateGateway = mockk<ExchangeRateGateway>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val service = CurrencyConversionService(
        userRepository, transactionRepository, exchangeRateGateway, eventPublisher
    )

    private val userId = UserId.generate()
    private val idempotencyKey = UUID.randomUUID()
    private val sourceMoney = Money(BigDecimal("100.00"), Currency.USD)
    private val user = User.create("test@example.com", "hash")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `convert returns existing transaction when idempotency key already exists`() {
        val existing = Transaction.create(userId, idempotencyKey, sourceMoney, Currency.EUR, BigDecimal("0.92"))
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns existing

        val result = service.convert(userId, idempotencyKey, sourceMoney, Currency.EUR)

        assertEquals(existing, result)
        verify(exactly = 0) { userRepository.findById(any()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }

    @Test
    fun `convert throws UserNotFoundException when user does not exist`() {
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null
        every { userRepository.findById(userId) } returns null

        assertThrows<UserNotFoundException> {
            service.convert(userId, idempotencyKey, sourceMoney, Currency.EUR)
        }
    }

    @Test
    fun `convert saves transaction and publishes event on success`() {
        every { transactionRepository.findByIdempotencyKey(idempotencyKey) } returns null
        every { userRepository.findById(userId) } returns user
        every { exchangeRateGateway.getRate(Currency.USD, Currency.EUR) } returns BigDecimal("0.92")
        val savedTx = slot<Transaction>()
        every { transactionRepository.save(capture(savedTx)) } answers { savedTx.captured }

        val result = service.convert(userId, idempotencyKey, sourceMoney, Currency.EUR)

        assertEquals(userId, result.userId)
        assertEquals(idempotencyKey, result.idempotencyKey)
        verify { eventPublisher.publishEvent(ofType<TransactionCreatedEvent>()) }
    }
}
