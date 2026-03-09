package com.fintech.currencyconverter.adapter.output.gateway

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import com.fasterxml.jackson.annotation.JsonProperty

@FeignClient(name = "currencyApi", url = "\${app.currency-api.url}")
interface CurrencyApiClient {

    @GetMapping("/latest")
    fun getLatestRates(
        @RequestParam("apikey") apiKey: String,
        @RequestParam("base_currency") baseCurrency: String,
        @RequestParam("currencies") currencies: String,
    ): CurrencyApiResponse
}

data class CurrencyApiResponse(
    val data: Map<String, CurrencyRate>,
)

data class CurrencyRate(
    @JsonProperty("value")
    val value: Double,
)

