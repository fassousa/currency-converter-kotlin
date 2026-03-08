package com.fintech.currencyconverter.adapter.input.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "API health check")
class HealthController(
    private val jdbcTemplate: JdbcTemplate,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${spring.application.version:1.0.0}") private val appVersion: String,
    @Value("\${CURRENCY_API_KEY:}") private val currencyApiKey: String,
) {

    @GetMapping
    @Operation(summary = "Check API health status", description = "Returns the health status of the API, database, cache, and external dependencies")
    fun health(): ResponseEntity<Map<String, Any>> {
        val checks = mapOf(
            "database" to checkDatabase(),
            "cache" to checkCache(),
            "external_api" to checkExternalApi(),
        )

        val allHealthy = checks.values.all { it["status"] == "up" }

        val body = mapOf(
            "status" to if (allHealthy) "healthy" else "unhealthy",
            "timestamp" to Instant.now().toString(),
            "version" to appVersion,
            "checks" to checks,
        )

        return if (allHealthy) ResponseEntity.ok(body)
        else ResponseEntity.status(503).body(body)
    }

    private fun checkDatabase(): Map<String, String> =
        runCatching {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            mapOf("status" to "up")
        }.getOrElse { e ->
            mapOf("status" to "down", "error" to (e.message ?: "unknown"))
        }

    private fun checkCache(): Map<String, String> =
        runCatching {
            val key = "health_check"
            redisTemplate.opsForValue().set(key, "1")
            redisTemplate.delete(key)
            mapOf("status" to "up")
        }.getOrElse { e ->
            mapOf("status" to "down", "error" to (e.message ?: "unknown"))
        }

    private fun checkExternalApi(): Map<String, String> =
        if (currencyApiKey.isNotBlank()) mapOf("status" to "up", "message" to "API key is configured")
        else mapOf("status" to "down", "message" to "API key is missing")
}

