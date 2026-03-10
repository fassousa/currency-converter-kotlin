package com.fintech.currencyconverter.adapter.output.gateway

import com.fintech.currencyconverter.domain.exception.ExchangeRateUnavailableException
import com.fintech.currencyconverter.domain.port.output.ExchangeRateGateway
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CurrencyApiGateway(
    private val currencyApiClient: CurrencyApiClient,
    @Value("\${app.currency-api.api-key}") private val apiKey: String,
) : ExchangeRateGateway,
    com.fintech.currencyconverter.port.outbound.ExchangeRateGateway {

    @Cacheable("exchangeRates", key = "#sourceCurrency + '_' + #targetCurrency")
    @CircuitBreaker(name = "exchangeRates", fallbackMethod = "fallback")
    @Retry(name = "exchangeRates")
    override fun getRate(sourceCurrency: String, targetCurrency: String): BigDecimal {
        val response = currencyApiClient.getLatestRates(
            apiKey = apiKey,
            baseCurrency = sourceCurrency,
            currencies = targetCurrency,
        )
        val rateData = response.data[targetCurrency]
            ?: throw ExchangeRateUnavailableException(
                "Rate not found for $sourceCurrency -> $targetCurrency",
            )
        return rateData.value
    }

    @Suppress("unused")
    private fun fallback(
        @Suppress("UNUSED_PARAMETER") sourceCurrency: String,
        @Suppress("UNUSED_PARAMETER") targetCurrency: String,
        e: Throwable,
    ): BigDecimal =
        throw ExchangeRateUnavailableException(
            "Exchange rate service unavailable: ${e.message}",
        )
}

