package com.fintech.currencyconverter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

/**
 * Application entry point.
 *
 * @SpringBootApplication: component scan + auto-configuration.
 * @EnableFeignClients: activates Feign proxy generation for all @FeignClient interfaces
 *   under this package. Feign is the HTTP client for the external CurrencyAPI adapter.
 */
@SpringBootApplication
@EnableFeignClients
class CurrencyConverterApplication

fun main(args: Array<String>) {
    runApplication<CurrencyConverterApplication>(*args)
}

