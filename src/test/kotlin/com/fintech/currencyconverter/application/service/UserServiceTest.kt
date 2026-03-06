package com.fintech.currencyconverter.application.service

import com.fintech.currencyconverter.domain.exception.AuthenticationException
import com.fintech.currencyconverter.domain.exception.EmailAlreadyRegisteredException
import com.fintech.currencyconverter.domain.model.User
import com.fintech.currencyconverter.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val service = UserService(userRepository, passwordEncoder)

    @Test
    fun `register throws EmailAlreadyRegisteredException when email taken`() {
        every { userRepository.existsByEmail("taken@example.com") } returns true

        assertThrows<EmailAlreadyRegisteredException> {
            service.register("taken@example.com", "password")
        }
    }

    @Test
    fun `register saves and returns new user`() {
        every { userRepository.existsByEmail("new@example.com") } returns false
        every { passwordEncoder.encode("password") } returns "hashed"
        val savedUser = slot<User>()
        every { userRepository.save(capture(savedUser)) } answers { savedUser.captured }

        val result = service.register("new@example.com", "password")

        assertEquals("new@example.com", result.email)
        assertEquals("hashed", result.passwordDigest)
    }

    @Test
    fun `authenticate throws AuthenticationException when user not found`() {
        every { userRepository.findByEmail("ghost@example.com") } returns null

        assertThrows<AuthenticationException> {
            service.authenticate("ghost@example.com", "password")
        }
    }

    @Test
    fun `authenticate throws AuthenticationException when password wrong`() {
        val user = User.create("user@example.com", "hash")
        every { userRepository.findByEmail("user@example.com") } returns user
        every { passwordEncoder.matches("wrong", "hash") } returns false

        assertThrows<AuthenticationException> {
            service.authenticate("user@example.com", "wrong")
        }
    }

    @Test
    fun `authenticate returns user on valid credentials`() {
        val user = User.create("user@example.com", "hash")
        every { userRepository.findByEmail("user@example.com") } returns user
        every { passwordEncoder.matches("correct", "hash") } returns true

        val result = service.authenticate("user@example.com", "correct")

        assertEquals(user, result)
    }
}

