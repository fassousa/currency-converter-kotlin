package com.fintech.currencyconverter.adapter.output.gateway

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "currencyApi", url = "\${app.currency-api.url}")
interface CurrencyApiClient {

    @GetMapping("/latest")
    fun getLatestRates(
        @RequestHeader("apikey") apiKey: String,
        @RequestParam("base") base: String,
        @RequestParam("symbols") symbols: String,
    ): CurrencyApiResponse
}

data class CurrencyApiResponse(
    val success: Boolean,
    val base: String,
    val rates: Map<String, Double>,
)

