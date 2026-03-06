package com.fintech.currencyconverter.adapter.output.gateway

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.fintech.currencyconverter.domain.exception.ExchangeRateUnavailableException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CurrencyApiGatewayCircuitBreakerTest {

    @TestConfiguration
    class MockRedisConfig {
        @Bean
        fun redisTemplate(): RedisTemplate<String, String> = mockk(relaxed = true)
    }

    companion object {
        private val wireMock = WireMockServer(wireMockConfig().dynamicPort())

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            wireMock.start()
            registry.add("app.currency-api.url") { wireMock.baseUrl() }
            registry.add("app.currency-api.api-key") { "test-key" }
            registry.add("spring.cache.type") { "none" }
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            if (wireMock.isRunning) wireMock.stop()
        }
    }

    @Autowired
    private lateinit var gateway: CurrencyApiGateway

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @BeforeEach
    fun reset() {
        circuitBreakerRegistry.circuitBreaker("exchangeRates").reset()
        wireMock.resetAll()
    }

    @Test
    fun `circuit opens after minimum-number-of-calls consecutive 500 responses`() {
        wireMock.stubFor(
            get(anyUrl())
                .willReturn(aResponse().withStatus(500)),
        )

        // Drive 5 failures — minimum-number-of-calls in test profile
        repeat(5) {
            assertThatThrownBy { gateway.getRate("USD", "EUR") }
                .isInstanceOf(ExchangeRateUnavailableException::class.java)
        }

        // Circuit must now be OPEN
        val cb = circuitBreakerRegistry.circuitBreaker("exchangeRates")
        assertEquals(CircuitBreaker.State.OPEN, cb.state)

        // Record HTTP requests made so far
        val requestsBeforeOpen = wireMock.allServeEvents.size

        // Next call — circuit is OPEN, no HTTP request should reach WireMock
        assertThatThrownBy { gateway.getRate("USD", "EUR") }
            .isInstanceOf(ExchangeRateUnavailableException::class.java)

        // WireMock must not have received any additional requests
        wireMock.verify(requestsBeforeOpen, getRequestedFor(urlPathMatching(".*")))
    }
}



